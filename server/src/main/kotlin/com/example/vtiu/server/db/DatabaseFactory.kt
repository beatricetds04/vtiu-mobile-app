package com.example.vtiu.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.toKotlinLocalDateTime
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

object DatabaseFactory {
    fun init() {
        println("--- DATABASE INITIALIZATION START ---")
        
        val dataSource = try {
            getDataSource()
        } catch (e: Exception) {
            println("DatabaseFactory: FATAL - Could not create DataSource: ${e.message}")
            return
        }

        Database.connect(dataSource)
        
        try {
            transaction {
                SchemaUtils.createMissingTablesAndColumns(
                    Admins, Users, StudentProfiles, TeacherProfiles,
                    Courses, Assignments, Quizzes, Exams,
                    StudentFeeTransactions, StudentFeeBalances,
                    Notifications, AppointmentBookings, AppointmentSlots,
                    AcademicCalendar, TimetableEntries, StudentCourseGrades,
                    Meetings, Questions, Options, StudentQuizSubmissions, CourseMaterials,
                    TeacherCourseAssignments, StudentCourseRegistrations, AttendanceRecords,
                    CourseAssessmentSchemes, AssignmentSubmissions, SemesterResultReleases,
                    SchoolSettings, ProgrammeFeeStructures, ChatMessages
                )

                // Seed SchoolSettings if empty, prioritizing Environment Variables for Agora
                if (SchoolSettings.selectAll().empty()) {
                    println("DatabaseFactory: Seeding default SchoolSettings...")
                    val currentYear = LocalDate.now().year.toString()
                    SchoolSettings.insert {
                        it[schoolName] = "VTIU"
                        it[currentAcademicYear] = currentYear
                        it[currentSemester] = "First"
                        it[paystackMode] = "test"
                        // SYNC: Pull from Env if possible to match Flask
                        it[agoraAppId] = System.getenv("AGORA_APP_ID") ?: "c79f6fe95bad487cafec43820f0200cb"
                        it[agoraAppCertificate] = System.getenv("AGORA_APP_CERTIFICATE") ?: ""
                        it[agoraWhiteboardId] = System.getenv("WHITEBOARD_APP_IDENTIFIER") ?: ""
                        it[agoraWhiteboardToken] = System.getenv("WHITEBOARD_SDK_TOKEN") ?: ""
                        it[updatedAt] = LocalDateTime.now().toKotlinLocalDateTime()
                    }
                }
            }
        } catch (e: Exception) {
            println("DatabaseFactory: ERROR - Table creation failed: ${e.message}")
        }
    }

    private fun getDataSource(): HikariDataSource {
        val databaseUrl = System.getenv("DATABASE_URL") ?: System.getenv("DATABASE_PUBLIC_URL")
        val pgUser = System.getenv("PGUSER") ?: System.getenv("POSTGRES_USER")
        val pgPass = System.getenv("PGPASSWORD") ?: System.getenv("POSTGRES_PASSWORD")
        val pgHost = System.getenv("PGHOST") ?: System.getenv("POSTGRES_HOST")
        val pgPort = System.getenv("PGPORT") ?: System.getenv("POSTGRES_PORT") ?: "5432"
        val pgDb = System.getenv("PGDATABASE") ?: System.getenv("POSTGRES_DB")

        if (!databaseUrl.isNullOrBlank()) {
            return try {
                val sanitizedUrl = databaseUrl.replace("postgresql://", "postgres://")
                val uri = URI(sanitizedUrl)
                val userInfo = uri.userInfo ?: ":"
                val userPass = userInfo.split(":")
                val user = userPass.getOrElse(0) { "" }
                val password = userPass.getOrElse(1) { "" }
                val host = uri.host
                val port = if (uri.port != -1) uri.port else 5432
                val path = uri.path
                
                val jdbcUrl = "jdbc:postgresql://$host:$port$path?sslmode=require"
                createHikariDataSource(jdbcUrl, user, password)
            } catch (e: Exception) {
                internalGetDataSource(pgHost, pgPort, pgDb, pgUser, pgPass)
            }
        }
        return internalGetDataSource(pgHost, pgPort, pgDb, pgUser, pgPass)
    }

    private fun internalGetDataSource(pgHost: String?, pgPort: String?, pgDb: String?, pgUser: String?, pgPass: String?): HikariDataSource {
        if (!pgUser.isNullOrBlank() && !pgPass.isNullOrBlank() && !pgHost.isNullOrBlank()) {
            val jdbcUrl = "jdbc:postgresql://$pgHost:$pgPort/$pgDb?sslmode=require"
            return createHikariDataSource(jdbcUrl, pgUser, pgPass)
        }
        val localUrl = System.getenv("JDBC_DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/vtiu"
        return createHikariDataSource(localUrl, "postgres", "password")
    }

    private fun createHikariDataSource(url: String, user: String, pass: String): HikariDataSource {
        return HikariDataSource(HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = url
            username = user
            password = pass
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        })
    }
}
