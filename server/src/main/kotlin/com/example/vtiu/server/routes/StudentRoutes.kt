package com.example.vtiu.server.routes

import com.example.vtiu.server.models.*
import com.example.vtiu.server.db.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun Route.studentRoutes() {
    route("/api") {
        // --- Courses & Registration ---
        get("/student/courses/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val courses = transaction {
                val userRow = Users.selectAll().where { Users.userId eq userId }.singleOrNull() ?: return@transaction emptyList<StudentCourseApi>()
                (StudentCourseRegistrations innerJoin Courses).selectAll().where { StudentCourseRegistrations.studentId eq userRow[Users.id] }.map {
                    StudentCourseApi(
                        id = it[Courses.id],
                        name = it[Courses.name],
                        code = it[Courses.code],
                        level = it[Courses.programmeLevel],
                        credits = it[Courses.creditHours]
                    )
                }
            }
            call.respond(courses)
        }

        get("/student/registration/available/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val available = transaction {
                val studentProfile = StudentProfiles.selectAll().where { StudentProfiles.userId eq userId }.singleOrNull() ?: return@transaction emptyList<CourseRegistrationApi>()
                val programme = studentProfile[StudentProfiles.currentProgramme]
                val level = studentProfile[StudentProfiles.programmeLevel].toString()
                
                Courses.selectAll().where { (Courses.programmeName eq programme) and (Courses.programmeLevel eq level) }.map {
                    CourseRegistrationApi(
                        id = it[Courses.id],
                        name = it[Courses.name],
                        code = it[Courses.code],
                        credits = it[Courses.creditHours],
                        isMandatory = it[Courses.isMandatory],
                        semester = it[Courses.semester],
                        academicYear = it[Courses.academicYear]
                    )
                }
            }
            call.respond(available)
        }

        post("/student/register") {
            val request = call.receive<RegisterCoursesRequest>()
            val success = transaction {
                val userRow = Users.selectAll().where { Users.userId eq request.userId }.singleOrNull() ?: return@transaction false
                StudentCourseRegistrations.deleteWhere { 
                    (studentId eq userRow[Users.id]) and (semester eq request.semester) and (academicYear eq request.academicYear) 
                }
                request.courseIds.forEach { cid ->
                    StudentCourseRegistrations.insert {
                        it[StudentCourseRegistrations.studentId] = userRow[Users.id]
                        it[StudentCourseRegistrations.courseId] = cid
                        it[StudentCourseRegistrations.semester] = request.semester
                        it[StudentCourseRegistrations.academicYear] = request.academicYear
                    }
                }
                true
            }
            if (success) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.BadRequest)
        }

        // --- Academic Calendar ---
        get("/calendar") {
            val events = transaction {
                AcademicCalendar.selectAll().map {
                    CalendarEventApi(
                        id = it[AcademicCalendar.id],
                        title = it[AcademicCalendar.label],
                        date = it[AcademicCalendar.date],
                        type = it[AcademicCalendar.breakType],
                        isWorkday = it[AcademicCalendar.isWorkday]
                    )
                }
            }
            call.respond(events)
        }

        // --- Timetable ---
        get("/student/timetable/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val entries = transaction {
                val studentProfile = StudentProfiles.selectAll().where { StudentProfiles.userId eq userId }.singleOrNull() ?: return@transaction emptyList<TimetableEntryApi>()
                val level = studentProfile[StudentProfiles.programmeLevel].toString()
                
                (TimetableEntries innerJoin Courses).selectAll().where { TimetableEntries.programmeLevel eq level }.map {
                    TimetableEntryApi(
                        id = it[TimetableEntries.id],
                        courseName = it[Courses.name],
                        dayOfWeek = it[TimetableEntries.dayOfWeek],
                        startTime = it[TimetableEntries.startTime],
                        endTime = it[TimetableEntries.endTime]
                    )
                }
            }
            call.respond(entries)
        }

        // --- Assessments & Quizzes ---
        get("/student/assessments/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val assessments = transaction {
                val userRow = Users.selectAll().where { Users.userId eq userId }.singleOrNull() ?: return@transaction emptyList<StudentAssessmentApi>()
                val list = mutableListOf<StudentAssessmentApi>()
                
                (StudentQuizSubmissions innerJoin Quizzes).selectAll().where { StudentQuizSubmissions.studentId eq userRow[Users.id] }.forEach {
                    list.add(StudentAssessmentApi(
                        type = "Quiz",
                        course = it[Quizzes.courseName],
                        title = it[Quizzes.title],
                        rawScore = it[StudentQuizSubmissions.score] ?: 0f,
                        maxScore = 0f, 
                        date = it[StudentQuizSubmissions.submittedAt].toString()
                    ))
                }
                
                (AssignmentSubmissions innerJoin Assignments).selectAll().where { AssignmentSubmissions.studentId eq userRow[Users.id] }.forEach {
                    list.add(StudentAssessmentApi(
                        type = "Assignment",
                        course = it[Assignments.courseName],
                        title = it[Assignments.title],
                        rawScore = it[AssignmentSubmissions.score] ?: 0f,
                        maxScore = it[Assignments.maxScore],
                        date = it[AssignmentSubmissions.submittedAt].toString(),
                        feedback = it[AssignmentSubmissions.feedback]
                    ))
                }
                list
            }
            call.respond(assessments)
        }

        get("/student/quizzes/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val list = transaction {
                val studentProfile = StudentProfiles.selectAll().where { StudentProfiles.userId eq userId }.singleOrNull() ?: return@transaction emptyList<QuizDetailApi>()
                val level = studentProfile[StudentProfiles.programmeLevel].toString()
                
                Quizzes.selectAll().where { Quizzes.programmeLevel eq level }.map {
                    QuizDetailApi(
                        id = it[Quizzes.id],
                        title = it[Quizzes.title],
                        courseName = it[Quizzes.courseName],
                        durationMinutes = it[Quizzes.durationMinutes],
                        maxScore = 0f, 
                        startDatetime = it[Quizzes.startDatetime].toString(),
                        endDatetime = it[Quizzes.endDatetime].toString(),
                        attemptsAllowed = it[Quizzes.attemptsAllowed]
                    )
                }
            }
            call.respond(list)
        }

        // --- Results & Transcripts ---

        // --- Fees & Transactions ---
        get("/fees/balance/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val balance = transaction {
                println("Fees: Checking balance for $userId")
                var studentRow = StudentFeeBalances.selectAll().where { StudentFeeBalances.studentId eq userId }.singleOrNull()
                
                if (studentRow == null) {
                    println("Fees: No balance record found for $userId. Attempting to initialize...")
                    // Try to initialize balance from ProgrammeFeeStructures
                    val profile = StudentProfiles.selectAll().where { StudentProfiles.userId eq userId }.singleOrNull()
                    if (profile != null) {
                        val prog = profile[StudentProfiles.currentProgramme]
                        val lvl = profile[StudentProfiles.programmeLevel].toString()
                        val format = profile[StudentProfiles.studyFormat]
                        val year = profile[StudentProfiles.academicYear] ?: ""
                        val rawSem = profile[StudentProfiles.semester] ?: ""
                        
                        // Map "1" to "First" and "2" to "Second" for consistency with fee structure table
                        val sem = when(rawSem) {
                            "1" -> "First"
                            "2" -> "Second"
                            else -> rawSem
                        }
                        
                        println("Fees: Searching for structure: Prog=$prog, Lvl=$lvl, Format=$format, Year=$year, Sem=$sem")
                        
                        val structure = ProgrammeFeeStructures.selectAll().where { 
                            (ProgrammeFeeStructures.programmeName eq prog) and 
                            (ProgrammeFeeStructures.programmeLevel eq lvl) and
                            (ProgrammeFeeStructures.studyFormat eq format) and
                            (ProgrammeFeeStructures.academicYear eq year) and
                            (ProgrammeFeeStructures.semester eq sem)
                        }.singleOrNull()
                        
                        if (structure != null) {
                            println("Fees: Found structure! Amount=${structure[ProgrammeFeeStructures.amount]}")
                            StudentFeeBalances.insert {
                                it[StudentFeeBalances.studentId] = userId
                                it[StudentFeeBalances.feeStructureId] = structure[ProgrammeFeeStructures.id]
                                it[StudentFeeBalances.programmeName] = prog
                                it[StudentFeeBalances.programmeLevel] = lvl
                                it[StudentFeeBalances.studyFormat] = format
                                it[StudentFeeBalances.academicYear] = year
                                it[StudentFeeBalances.semester] = sem
                                it[StudentFeeBalances.amountDue] = structure[ProgrammeFeeStructures.amount]
                                it[StudentFeeBalances.amountPaid] = 0.0f
                                it[StudentFeeBalances.isPaid] = false
                                it[StudentFeeBalances.createdAt] = LocalDateTime.now().toKotlinLocalDateTime()
                                it[StudentFeeBalances.updatedAt] = LocalDateTime.now().toKotlinLocalDateTime()
                            }
                            // Re-fetch
                            studentRow = StudentFeeBalances.selectAll().where { StudentFeeBalances.studentId eq userId }.singleOrNull()
                        } else {
                            println("Fees: FAILED to find a matching fee structure for student profile.")
                        }
                    } else {
                        println("Fees: FAILED to find student profile for $userId")
                    }
                }

                if (studentRow != null) {
                    FeeBalanceApi(
                        amountDue = studentRow[StudentFeeBalances.amountDue].toDouble(),
                        amountPaid = studentRow[StudentFeeBalances.amountPaid].toDouble(),
                        balance = (studentRow[StudentFeeBalances.amountDue] - studentRow[StudentFeeBalances.amountPaid]).toDouble()
                    )
                } else {
                    println("Fees: Returning 0.00 because no balance could be determined.")
                    FeeBalanceApi(0.0, 0.0, 0.0)
                }
            }
            call.respond(balance)
        }

        get("/student/fees/transactions/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val txns = transaction {
                val userRow = Users.selectAll().where { Users.userId eq userId }.singleOrNull() ?: return@transaction emptyList<FeeTransactionApi>()
                StudentFeeTransactions.selectAll().where { StudentFeeTransactions.studentId eq userRow[Users.id] }.map {
                    FeeTransactionApi(
                        id = it[StudentFeeTransactions.id],
                        amount = it[StudentFeeTransactions.amount].toDouble(),
                        description = it[StudentFeeTransactions.description],
                        date = it[StudentFeeTransactions.timestamp].toString(),
                        status = if (it[StudentFeeTransactions.isApproved]) "Approved" else "Pending",
                        academicYear = it[StudentFeeTransactions.academicYear],
                        semester = it[StudentFeeTransactions.semester]
                    )
                }
            }
            call.respond(txns)
        }

        post("/student/fees/pay") {
            val request = call.receive<PayFeesRequest>()
            val success = transaction {
                val userRow = Users.selectAll().where { Users.userId eq request.userId }.singleOrNull() ?: return@transaction false
                StudentFeeTransactions.insert {
                    it[studentId] = userRow[Users.id]
                    it[amount] = request.amount.toFloat()
                    it[description] = request.description
                    it[academicYear] = request.academicYear
                    it[semester] = request.semester
                    it[timestamp] = LocalDateTime.now().toKotlinLocalDateTime()
                    it[isApproved] = false
                }.insertedCount > 0
            }
            if (success) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.InternalServerError)
        }

        get("/notifications/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val list = transaction {
                Notifications.selectAll().where { Notifications.userId eq userId }.map {
                    NotificationApi(
                        id = it[Notifications.id],
                        title = it[Notifications.title],
                        message = it[Notifications.message],
                        date = it[Notifications.date] ?: "",
                        isRead = it[Notifications.isRead]
                    )
                }
            }
            call.respond(list)
        }
    }
}
