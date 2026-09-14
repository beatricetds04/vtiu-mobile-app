package com.example.vtiu.server.routes

import com.example.vtiu.server.models.*
import com.example.vtiu.server.db.*
import com.example.vtiu.server.paystackClient
import com.example.vtiu.server.utils.AgoraTokenBuilder
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

fun generateRoomId(): String {
    return "meeting-${UUID.randomUUID().toString().replace("-", "")}"
}

fun Route.vClassRoutes() {
    route("/api") {
        // --- Virtual Class Materials ---
        get("/vclass/materials/{courseId}") {
            val courseId = call.parameters["courseId"]?.toIntOrNull() ?: 0
            val materials = transaction {
                val courseName = Courses.selectAll().where { Courses.id eq courseId }.singleOrNull()?.get(Courses.name) ?: ""
                CourseMaterials.selectAll().where { CourseMaterials.courseName eq courseName }.map {
                    MaterialApi(
                        id = it[CourseMaterials.id],
                        title = it[CourseMaterials.title],
                        courseName = it[CourseMaterials.courseName],
                        fileUrl = it[CourseMaterials.filename],
                        fileType = it[CourseMaterials.fileType],
                        uploadDate = it[CourseMaterials.uploadDate].toString()
                    )
                }
            }
            call.respond(materials)
        }

        get("/vclass/material/{materialId}") {
            val materialId = call.parameters["materialId"]?.toIntOrNull() ?: 0
            val material = transaction {
                CourseMaterials.selectAll().where { CourseMaterials.id eq materialId }.singleOrNull()?.let {
                    MaterialApi(
                        id = it[CourseMaterials.id],
                        title = it[CourseMaterials.title],
                        courseName = it[CourseMaterials.courseName],
                        fileUrl = it[CourseMaterials.filename],
                        fileType = it[CourseMaterials.fileType],
                        uploadDate = it[CourseMaterials.uploadDate].toString()
                    )
                }
            }
            if (material != null) call.respond(material) else call.respond(HttpStatusCode.NotFound)
        }

        // --- Assignments ---
        get("/vclass/assignments/{courseId}") {
            val courseId = call.parameters["courseId"]?.toIntOrNull() ?: 0
            val list = transaction {
                Assignments.selectAll().where { Assignments.courseId eq courseId }.map {
                    VClassAssignmentApi(
                        id = it[Assignments.id],
                        title = it[Assignments.title],
                        courseName = it[Assignments.courseName],
                        dueDate = it[Assignments.dueDate].toString(),
                        maxScore = it[Assignments.maxScore]
                    )
                }
            }
            call.respond(list)
        }

        post("/assignments/submit") {
            val data = call.receive<Map<String, String>>()
            val studentId = data["student_id"] ?: ""
            val assignmentId = data["assignment_id"]?.toIntOrNull() ?: 0
            val filename = data["filename"] ?: ""

            val success = transaction {
                val userRow = Users.selectAll().where { Users.userId eq studentId }.singleOrNull() ?: return@transaction false
                AssignmentSubmissions.insert {
                    it[AssignmentSubmissions.assignmentId] = assignmentId
                    it[AssignmentSubmissions.studentId] = userRow[Users.id]
                    it[AssignmentSubmissions.filename] = filename
                    it[AssignmentSubmissions.originalName] = filename
                    it[AssignmentSubmissions.submittedAt] = LocalDateTime.now().toKotlinLocalDateTime()
                }.insertedCount > 0
            }
            if (success) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.BadRequest)
        }

        // --- Meetings ---
        post("/meetings/create") {
            val request = call.receive<CreateMeetingRequest>()
            val success = transaction {
                val userRow = Users.selectAll().where { Users.userId eq request.hostUserId }.singleOrNull() ?: return@transaction false
                val courseRow = Courses.selectAll().where { Courses.name eq request.courseName }.singleOrNull() ?: return@transaction false
                
                Meetings.insert {
                    it[title] = request.title
                    it[hostId] = userRow[Users.id]
                    it[courseId] = courseRow[Courses.id]
                    it[meetingCode] = generateRoomId()
                    it[scheduledStart] = LocalDateTime.parse(request.start.replace(" ", "T")).toKotlinLocalDateTime()
                    it[scheduledEnd] = LocalDateTime.parse(request.end.replace(" ", "T")).toKotlinLocalDateTime()
                    it[createdAt] = LocalDateTime.now().toKotlinLocalDateTime()
                }.insertedCount > 0
            }
            if (success) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.BadRequest)
        }

        get("/vclass/meetings/{courseId}") {
            val courseId = call.parameters["courseId"]?.toIntOrNull() ?: 0
            val meetings = transaction {
                (Meetings innerJoin Courses).selectAll().where { Meetings.courseId eq courseId }.map {
                    val hostId = it[Meetings.hostId]
                    val teacher = if (hostId != null) {
                        Users.selectAll().where { Users.id eq hostId }.singleOrNull()
                    } else {
                        (TeacherCourseAssignments innerJoin Users)
                            .selectAll().where { TeacherCourseAssignments.courseId eq courseId }
                            .singleOrNull()
                    }

                    VClassMeetingApi(
                        id = it[Meetings.id],
                        title = it[Meetings.title],
                        courseName = it[Courses.name],
                        teacherName = if (teacher != null) "${teacher[Users.firstName]} ${teacher[Users.lastName]}" else "Teacher",
                        hostId = hostId ?: teacher?.get(Users.id),
                        meetingCode = it[Meetings.meetingCode],
                        start = it[Meetings.scheduledStart].toString(),
                        end = it[Meetings.scheduledEnd].toString(),
                        isLive = true
                    )
                }
            }
            call.respond(meetings)
        }

        // --- Quizzes ---
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

        get("/vclass/quiz/{quizId}") {
            val quizId = call.parameters["quizId"]?.toIntOrNull() ?: 0
            val quiz = transaction {
                val q = Quizzes.selectAll().where { Quizzes.id eq quizId }.singleOrNull() ?: return@transaction null
                val questionsList = Questions.selectAll().where { Questions.quizId eq quizId }.map { row ->
                    QuizQuestionApi(
                        id = row[Questions.id],
                        questionText = row[Questions.text],
                        questionType = row[Questions.questionType],
                        points = row[Questions.points],
                        options = Options.selectAll().where { Options.questionId eq row[Questions.id] }.map { opt ->
                            QuizOptionApi(
                                id = opt[Options.id],
                                text = opt[Options.text],
                                isCorrect = false
                            )
                        }
                    )
                }
                QuizDetailApi(
                    id = q[Quizzes.id],
                    title = q[Quizzes.title],
                    courseName = q[Quizzes.courseName],
                    durationMinutes = q[Quizzes.durationMinutes],
                    maxScore = questionsList.sumOf { it.points.toDouble() }.toFloat(),
                    startDatetime = q[Quizzes.startDatetime].toString(),
                    endDatetime = q[Quizzes.endDatetime].toString(),
                    attemptsAllowed = q[Quizzes.attemptsAllowed],
                    questions = questionsList
                )
            }
            if (quiz != null) call.respond(quiz) else call.respond(HttpStatusCode.NotFound)
        }

        // --- Exams ---
        get("/student/exams/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val list = transaction {
                val studentProfile = StudentProfiles.selectAll().where { StudentProfiles.userId eq userId }.singleOrNull() ?: return@transaction emptyList<ExamSubmissionApi>()
                val level = studentProfile[StudentProfiles.programmeLevel].toString()
                
                Exams.selectAll().where { Exams.programmeLevel eq level }.map {
                    ExamSubmissionApi(
                        id = it[Exams.id],
                        studentName = "N/A",
                        examTitle = it[Exams.title],
                        score = null,
                        submittedAt = ""
                    )
                }
            }
            call.respond(list)
        }

        get("/exam/{examId}") {
            val examId = call.parameters["examId"]?.toIntOrNull() ?: 0
            val exam = transaction {
                Exams.selectAll().where { Exams.id eq examId }.singleOrNull()?.let {
                    mapOf(
                        "id" to it[Exams.id],
                        "title" to it[Exams.title],
                        "duration_minutes" to it[Exams.durationMinutes]
                    )
                }
            }
            if (exam != null) call.respond(exam) else call.respond(HttpStatusCode.NotFound)
        }

        get("/vclass/meeting/{meetingId}") {
            val meetingId = call.parameters["meetingId"]?.toIntOrNull() ?: 0
            val meeting = transaction {
                val row = (Meetings leftJoin Courses).selectAll().where { Meetings.id eq meetingId }.singleOrNull()
                if (row == null) return@transaction null

                val hostIdValue = row[Meetings.hostId]
                val teacherRow = if (hostIdValue != null) {
                    Users.selectAll().where { Users.id eq hostIdValue }.singleOrNull()
                } else {
                    val cId = row[Meetings.courseId]
                    if (cId != null) {
                        (TeacherCourseAssignments innerJoin Users)
                            .selectAll().where { TeacherCourseAssignments.courseId eq cId }
                            .singleOrNull()
                    } else null
                }

                VClassMeetingApi(
                    id = row[Meetings.id],
                    title = row[Meetings.title],
                    courseName = row.getOrNull(Courses.name) ?: "General",
                    teacherName = if (teacherRow != null) "${teacherRow[Users.firstName]} ${teacherRow[Users.lastName]}" else "LMS Teacher",
                    hostId = hostIdValue ?: teacherRow?.get(Users.id),
                    meetingCode = row[Meetings.meetingCode],
                    start = row[Meetings.scheduledStart]?.toString() ?: "",
                    end = row[Meetings.scheduledEnd]?.toString() ?: "",
                    isLive = true
                )
            }
            if (meeting != null) call.respond(meeting) else call.respond(HttpStatusCode.NotFound, "Meeting not found")
        }

        // --- Whiteboard (Excalidraw) ---
        get("/vclass/whiteboard/{meetingId}") {
            val meetingId = call.parameters["meetingId"]?.toIntOrNull() ?: 0
            try {
                val roomUuid = transaction {
                    val meeting = Meetings.selectAll().where { Meetings.id eq meetingId }.singleOrNull()
                    var uuid = meeting?.get(Meetings.whiteboardRoomUuid)
                    if (uuid == null) {
                        val roomId = UUID.randomUUID().toString().replace("-", "").take(20)
                        val key = UUID.randomUUID().toString().replace("-", "").take(22)
                        uuid = "$roomId,$key"
                        Meetings.update({ Meetings.id eq meetingId }) { it[whiteboardRoomUuid] = uuid }
                    }
                    uuid
                }
                call.respond(WhiteboardRoomResponse(type = "excalidraw", roomUrl = "https://excalidraw.com/#room=$roomUuid", roomUuid = roomUuid!!))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Whiteboard Error: ${e.message}")
            }
        }

        // SYNC: Agora Token generation moved to Flask (Web) Backend as requested.
        // The Android App now calls /api/agora/token on the Flask server.

        // --- Student Dashboard Views ---
        get("/student/vclass/meetings/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val meetings = transaction {
                val userRow = Users.selectAll().where { Users.userId eq userId }.singleOrNull() ?: return@transaction emptyList<VClassMeetingApi>()
                val registeredCourseIds = StudentCourseRegistrations.selectAll().where { StudentCourseRegistrations.studentId eq userRow[Users.id] }.map { it[StudentCourseRegistrations.courseId] }
                if (registeredCourseIds.isEmpty()) return@transaction emptyList<VClassMeetingApi>()

                (Meetings leftJoin Courses).selectAll().where { Meetings.courseId inList registeredCourseIds }.orderBy(Meetings.scheduledStart, SortOrder.DESC).map {
                    val meetingHostId = it[Meetings.hostId]
                    val teacher = if (meetingHostId != null) {
                        Users.selectAll().where { Users.id eq meetingHostId }.singleOrNull()
                    } else {
                        (TeacherCourseAssignments innerJoin TeacherProfiles innerJoin Users).selectAll().where { TeacherCourseAssignments.courseId eq it[Courses.id] }.singleOrNull()
                    }
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val startStr = it[Meetings.scheduledStart]?.let { dt -> LocalDateTime.of(dt.year, dt.monthNumber, dt.dayOfMonth, dt.hour, dt.minute, dt.second).format(formatter) } ?: ""
                    val endStr = it[Meetings.scheduledEnd]?.let { dt -> LocalDateTime.of(dt.year, dt.monthNumber, dt.dayOfMonth, dt.hour, dt.minute, dt.second).format(formatter) } ?: ""

                    VClassMeetingApi(
                        id = it[Meetings.id],
                        title = it[Meetings.title],
                        courseName = it.getOrNull(Courses.name) ?: "General Session",
                        teacherName = if (teacher != null) "${teacher[Users.firstName]} ${teacher[Users.lastName]}" else "Instructor",
                        hostId = meetingHostId ?: teacher?.get(Users.id),
                        meetingCode = it[Meetings.meetingCode],
                        start = startStr,
                        end = endStr,
                        isLive = true
                    )
                }
            }
            call.respond(meetings)
        }

        get("/student/vclass/materials/{userId}") {
            val userId = call.parameters["userId"] ?: ""
            val materials = transaction {
                val studentProfile = StudentProfiles.selectAll().where { StudentProfiles.userId eq userId }.singleOrNull() ?: return@transaction emptyList<MaterialApi>()
                val programme = studentProfile[StudentProfiles.currentProgramme]
                val level = studentProfile[StudentProfiles.programmeLevel].toString()
                CourseMaterials.selectAll().where { (CourseMaterials.programmeName eq programme) and (CourseMaterials.programmeLevel eq level) }.map {
                    MaterialApi(id = it[CourseMaterials.id], title = it[CourseMaterials.title], courseName = it[CourseMaterials.courseName], fileUrl = it[CourseMaterials.filename], fileType = it[CourseMaterials.fileType], uploadDate = it[CourseMaterials.uploadDate].toString())
                }
            }
            call.respond(materials)
        }
    }
}
