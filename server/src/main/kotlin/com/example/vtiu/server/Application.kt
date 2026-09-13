package com.example.vtiu.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import io.ktor.server.websocket.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.example.vtiu.server.db.*
import com.example.vtiu.server.routes.*
import com.example.vtiu.server.redis.RedisFactory
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.selectAll

val paystackClient = HttpClient(CIO) {
    install(ClientContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }
}

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize Redis & Database in the background to prevent blocking server startup
    launch {
        try {
            RedisFactory.init()
            initChatRedisSubscriber()
            DatabaseFactory.init()
        } catch (e: Exception) {
            println("Failed to initialize server components: ${e.message}")
        }
    }
    install(WebSockets) {
        pingPeriod = java.time.Duration.ofSeconds(15)
        timeout = java.time.Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "Unknown error")))
        }
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
    }

    routing {
        get("/") {
            // Returns a simple 200 OK so Flask knows Ktor is alive
            call.respond(HttpStatusCode.OK, mapOf("status" to "Ktor is running smoothly"))
        }
        
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }

        authRoutes()
        studentRoutes()
        teacherRoutes()
        vClassRoutes()
        financeRoutes()
        appointmentRoutes()
        chatRoutes()
        
        get("/api/settings/agora") {
            val settings = transaction { SchoolSettings.selectAll().singleOrNull() }
            if (settings != null) {
                call.respond(mapOf("appId" to settings[SchoolSettings.agoraAppId]))
            } else {
                call.respond(mapOf("appId" to "c79f6fe95bad487cafec43820f0200cb"))
            }
        }
    }
}
