package com.example.vtiu.server.routes

import com.example.vtiu.server.models.*
import com.example.vtiu.server.db.*
import com.example.vtiu.server.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import io.ktor.client.call.*

fun Route.financeRoutes() {
    route("/api/finance") {
        // --- Paystack Integration ---
        
        post("/paystack/initialize") {
            val requestData = call.receive<Map<String, String>>()
            val userId = requestData["user_id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "User ID missing")
            val amountGhs = requestData["amount"]?.toDoubleOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "Amount missing")
            val email = if (requestData["email"].isNullOrBlank()) "student@vtiu.edu" else requestData["email"]!!
            
            val secretKey = transaction {
                val settings = SchoolSettings.selectAll().singleOrNull()
                val mode = settings?.get(SchoolSettings.paystackMode) ?: "test"
                val dbKey = if (mode == "live") settings?.get(SchoolSettings.paystackLiveSecretKey) else settings?.get(SchoolSettings.paystackTestSecretKey)
                
                // Priority: Specific Env Var > Generic Env Var > Database Setting
                val envTestKey = System.getenv("PAYSTACK_TEST_SECRET_KEY")
                val envLiveKey = System.getenv("PAYSTACK_LIVE_SECRET_KEY")
                val envGenericKey = System.getenv("PAYSTACK_SECRET_KEY")

                val finalKey = when {
                    mode == "live" && !envLiveKey.isNullOrBlank() -> envLiveKey
                    mode == "test" && !envTestKey.isNullOrBlank() -> envTestKey
                    !envGenericKey.isNullOrBlank() -> envGenericKey
                    !dbKey.isNullOrBlank() -> dbKey
                    else -> "" 
                }
                finalKey
            }

            if (secretKey.isBlank()) {
                println("Paystack Error: Secret key is blank. Checked PAYSTACK_TEST_SECRET_KEY, PAYSTACK_LIVE_SECRET_KEY, and PAYSTACK_SECRET_KEY.")
                return@post call.respond(HttpStatusCode.InternalServerError, "Paystack API Key not configured")
            }

            val amountKobo = (amountGhs * 100).toLong()
            val reference = "VTIU_${System.currentTimeMillis()}"
            
            val paystackReq = PaystackInitializeRequest(
                email = email,
                amount = amountKobo.toString(),
                reference = reference,
                callbackUrl = "https://vtiu-lms-production-770d.up.railway.app/student/paystack/callback",
                metadata = mapOf("user_id" to userId)
            )

            try {
                val response: HttpResponse = paystackClient.post("https://api.paystack.co/transaction/initialize") {
                    setBody(paystackReq)
                    header(HttpHeaders.Authorization, "Bearer $secretKey")
                    contentType(ContentType.Application.Json)
                }

                if (response.status == HttpStatusCode.OK) {
                    val body = response.body<PaystackInitializeResponse>()
                    call.respond(body)
                } else {
                    val error = response.bodyAsText()
                    call.respond(HttpStatusCode.InternalServerError, "Paystack Error: $error")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Server Error: ${e.message}")
            }
        }

        get("/paystack/verify/{reference}") {
            val reference = call.parameters["reference"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            
            val secretKey = transaction {
                val settings = SchoolSettings.selectAll().singleOrNull()
                val mode = settings?.get(SchoolSettings.paystackMode) ?: "test"
                val dbKey = if (mode == "live") settings?.get(SchoolSettings.paystackLiveSecretKey) else settings?.get(SchoolSettings.paystackTestSecretKey)
                
                val envTestKey = System.getenv("PAYSTACK_TEST_SECRET_KEY")
                val envLiveKey = System.getenv("PAYSTACK_LIVE_SECRET_KEY")
                val envGenericKey = System.getenv("PAYSTACK_SECRET_KEY")

                when {
                    mode == "live" && !envLiveKey.isNullOrBlank() -> envLiveKey
                    mode == "test" && !envTestKey.isNullOrBlank() -> envTestKey
                    !envGenericKey.isNullOrBlank() -> envGenericKey
                    !dbKey.isNullOrBlank() -> dbKey
                    else -> ""
                }
            }

            if (secretKey.isBlank()) {
                return@get call.respond(HttpStatusCode.InternalServerError, "Paystack API Key not configured")
            }

            try {
                val response: HttpResponse = paystackClient.get("https://api.paystack.co/transaction/verify/$reference") {
                    header(HttpHeaders.Authorization, "Bearer $secretKey")
                }

                if (response.status == HttpStatusCode.OK) {
                    val body = response.body<PaystackVerifyResponse>()
                    if (body.data?.status == "success") {
                        processSuccessfulPayment(body.data)
                    }
                    call.respond(body)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Verification Failed")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
            }
        }

        // --- Standard Finance Endpoints ---
        get("/summary") {
            val summary = transaction {
                val totalRevenue = StudentFeeTransactions.selectAll().where { StudentFeeTransactions.isApproved eq true }
                    .sumOf { it[StudentFeeTransactions.amount].toDouble() }
                
                val outstanding = StudentFeeBalances.selectAll()
                    .sumOf { (it[StudentFeeBalances.amountDue] - it[StudentFeeBalances.amountPaid]).toDouble() }
                
                val pendingCount = StudentFeeTransactions.selectAll().where { StudentFeeTransactions.isApproved eq false }.count()
                
                mapOf(
                    "total_revenue" to totalRevenue,
                    "outstanding_balance" to outstanding,
                    "pending_count" to pendingCount,
                    "collection_rate" to if (totalRevenue + outstanding > 0) (totalRevenue / (totalRevenue + outstanding) * 100) else 0.0
                )
            }
            call.respond(summary)
        }

        get("/payments") {
            val payments = transaction {
                (StudentFeeTransactions innerJoin Users).selectAll().map {
                    mapOf(
                        "id" to it[StudentFeeTransactions.id],
                        "student_name" to "${it[Users.firstName]} ${it[Users.lastName]}",
                        "amount" to it[StudentFeeTransactions.amount],
                        "status" to if (it[StudentFeeTransactions.isApproved]) "Approved" else "Pending",
                        "date" to it[StudentFeeTransactions.timestamp].toString()
                    )
                }
            }
            call.respond(payments)
        }

        post("/approve-payment/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val success = transaction {
                val txn = StudentFeeTransactions.selectAll().where { StudentFeeTransactions.id eq id }.singleOrNull() ?: return@transaction false
                if (txn[StudentFeeTransactions.isApproved]) return@transaction false

                StudentFeeTransactions.update({ StudentFeeTransactions.id eq id }) {
                    it[isApproved] = true
                    it[reviewedByAdminId] = 1
                }

                val studentId = txn[StudentFeeTransactions.studentId]
                val amount = txn[StudentFeeTransactions.amount]
                
                val user = Users.selectAll().where { Users.id eq studentId }.single()
                StudentFeeBalances.update({ StudentFeeBalances.studentId eq user[Users.userId] }) {
                    with(SqlExpressionBuilder) {
                        it.update(amountPaid, amountPaid + amount)
                    }
                }
                true
            }
            if (success) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.BadRequest)
        }
    }

    // --- New Paystack Webhook and Callback URLs ---
    post("/api/paystack/webhook") {
        val body = call.receive<Map<String, Any?>>()
        val event = body["event"] as? String
        val data = body["data"] as? Map<String, Any?>
        
        if (event == "charge.success" && data != null) {
            val reference = data["reference"] as? String ?: ""
            val amountKobo = (data["amount"] as? Number)?.toLong() ?: 0L
            val metadata = data["metadata"] as? Map<String, String?>
            
            processSuccessfulPayment(PaystackVerifyData(
                status = "success",
                reference = reference,
                amount = amountKobo,
                metadata = metadata?.mapValues { it.value ?: "" }
            ))
        }
        call.respond(HttpStatusCode.OK)
    }

    get("/student/paystack/callback") {
        call.respondText("<html><body><h2>Payment Successful!</h2><p>Your payment has been received. You can now close this window and return to the VTIU app.</p></body></html>", ContentType.Text.Html)
    }
}

private fun processSuccessfulPayment(data: PaystackVerifyData) {
    val userId = data.metadata?.get("user_id") ?: ""
    val amountGhs = data.amount / 100.0
    
    transaction {
        val userRow = Users.selectAll().where { Users.userId eq userId }.singleOrNull() ?: return@transaction
        
        // SYNC: Fetch current settings to match Flask's logic
        val settings = SchoolSettings.selectAll().singleOrNull()
        val currentYear = settings?.get(SchoolSettings.currentAcademicYear) ?: "2024/2025"
        val currentSem = settings?.get(SchoolSettings.currentSemester) ?: "First"
        
        StudentFeeTransactions.insert {
            it[studentId] = userRow[Users.id]
            it[amount] = amountGhs.toFloat()
            it[description] = "Online Payment - Paystack (${data.reference})"
            it[timestamp] = LocalDateTime.now().toKotlinLocalDateTime()
            it[isApproved] = true
            it[academicYear] = currentYear
            it[semester] = currentSem
        }

        StudentFeeBalances.update({ StudentFeeBalances.studentId eq userId }) {
            with(SqlExpressionBuilder) {
                it.update(amountPaid, amountPaid + amountGhs.toFloat())
            }
        }
    }
}
