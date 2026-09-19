package com.example.vtiu.data.repository

import com.example.vtiu.data.model.api.*
import com.example.vtiu.data.local.room.dao.ProfileDao
import com.example.vtiu.data.local.room.dao.TimetableDao
import com.example.vtiu.data.local.room.dao.CourseDao
import com.example.vtiu.data.mapper.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LmsRepository @Inject constructor(
    private val client: HttpClient,
    @javax.inject.Named("baseUrl") private val baseUrl: String,
    private val profileDao: ProfileDao,
    private val timetableDao: TimetableDao,
    private val courseDao: CourseDao
) {
    suspend fun testConnection(): String {
        return try {
            val response: HttpResponse = client.get("$baseUrl/")
            "Success: ${response.status}"
        } catch (e: Exception) {
            "Failed: ${e.message}"
        }
    }

    suspend fun login(request: LoginRequest): LoginResponse {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Unauthorized) {
                response.body()
            } else {
                val errorBody = response.bodyAsText()
                LoginResponse(success = false, message = "Server Error (${response.status}): $errorBody")
            }
        } catch (e: Exception) {
            LoginResponse(success = false, message = e.message ?: "Network error")
        }
    }

    suspend fun getProfile(role: String, userId: String): ProfileResponse {
        return try {
            val response: ProfileResponse = client.get("$baseUrl/api/profile/$role/$userId").body()
            if (response.success && response.profile != null) {
                profileDao.saveProfile(response.profile.toEntity())
            }
            response
        } catch (e: Exception) {
            val cachedProfile = profileDao.getProfile(userId)
            if (cachedProfile != null) {
                ProfileResponse(success = true, profile = cachedProfile.toDomain())
            } else {
                ProfileResponse(success = false, message = e.message)
            }
        }
    }

    suspend fun getTeacherClasses(userId: String): List<TeacherClassApi> {
        return try {
            client.get("$baseUrl/api/teacher/classes/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudentCourses(userId: String): List<StudentCourseApi> {
        return try {
            val response: List<StudentCourseApi> = client.get("$baseUrl/api/student/courses/$userId").body()
            if (response.isNotEmpty()) {
                courseDao.deleteCourses(userId)
                courseDao.saveCourses(response.map { it.toEntity(userId) })
            }
            response
        } catch (e: Exception) {
            courseDao.getCourses(userId).map { it.toApi() }
        }
    }

    suspend fun getCalendar(): List<CalendarEventApi> {
        return try {
            client.get("$baseUrl/api/calendar").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMaterials(courseId: Int): List<MaterialApi> {
        return try {
            client.get("$baseUrl/api/vclass/materials/$courseId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVClassAssignments(courseId: Int): List<VClassAssignmentApi> {
        return try {
            client.get("$baseUrl/api/vclass/assignments/$courseId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudentVClassAssignments(userId: String): List<VClassAssignmentApi> {
        return try {
            client.get("$baseUrl/api/student/vclass/assignments/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudentVClassMaterials(userId: String): List<MaterialApi> {
        return try {
            client.get("$baseUrl/api/student/vclass/materials/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTeacherVClassMaterials(userId: String): List<MaterialApi> {
        return try {
            client.get("$baseUrl/api/teacher/vclass/materials/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteMaterial(materialId: Int): Boolean {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/materials/delete/$materialId")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getVClassMeetings(courseId: Int): List<VClassMeetingApi> {
        return try {
            client.get("$baseUrl/api/vclass/meetings/$courseId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMeetingDetail(meetingId: Int): VClassMeetingApi? {
        return try {
            val url = "$baseUrl/api/vclass/meeting/$meetingId"
            println("VClass: Calling API: $url")
            val response: HttpResponse = client.get(url)
            println("VClass: API Status: ${response.status}")
            if (response.status == HttpStatusCode.OK) {
                response.body<VClassMeetingApi>()
            } else {
                null
            }
        } catch (e: Exception) {
            println("VClass: API Error: ${e.message}")
            null
        }
    }

    suspend fun joinMeetingByCode(roomCode: String): JoinMeetingByCodeApi? {
        return try {
            client.get("$baseUrl/vclass/api/meeting/join-by-code") {
                parameter("room_code", roomCode)
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStudentQuizzes(userId: String): List<QuizDetailApi> {
        return try {
            client.get("$baseUrl/api/student/quizzes/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudentVClassMeetings(userId: String): List<VClassMeetingApi> {
        return try {
            client.get("$baseUrl/api/student/vclass/meetings/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun submitAssignment(userId: String, assignmentId: Int, filename: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/assignments/submit") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("student_id" to userId, "assignment_id" to assignmentId, "filename" to filename))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getQuizDetail(quizId: Int): QuizDetailApi? {
        return try {
            client.get("$baseUrl/api/vclass/quiz/$quizId").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMaterialDetail(materialId: Int): MaterialApi? {
        return try {
            client.get("$baseUrl/api/vclass/material/$materialId").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFeeBalance(userId: String): FeeBalanceApi {
        return try {
            client.get("$baseUrl/api/fees/balance/$userId").body()
        } catch (e: Exception) {
            FeeBalanceApi(0.0, 0.0, 0.0)
        }
    }

    suspend fun getStudentTimetable(userId: String): List<TimetableEntryApi> {
        return try {
            val response: List<TimetableEntryApi> = client.get("$baseUrl/api/student/timetable/$userId").body()
            if (response.isNotEmpty()) {
                timetableDao.deleteTimetable(userId)
                timetableDao.saveTimetable(response.map { it.toEntity(userId) })
            }
            response
        } catch (e: Exception) {
            timetableDao.getTimetable(userId).map { it.toApi() }
        }
    }

    suspend fun getTeacherTimetable(userId: String): List<TimetableEntryApi> {
        return try {
            client.get("$baseUrl/api/teacher/timetable/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudentResults(userId: String): List<StudentResultApi> {
        return try {
            client.get("$baseUrl/api/student/results/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudentResultsFull(userId: String): List<StudentFullResultApi> {
        return try {
            client.get("$baseUrl/api/student/results/full/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudentAssessments(userId: String): List<StudentAssessmentApi> {
        return try {
            client.get("$baseUrl/api/student/assessments/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getNotifications(userId: String): List<NotificationApi> {
        return try {
            client.get("$baseUrl/api/notifications/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAppointmentSlots(): List<AppointmentSlotApi> {
        return try {
            client.get("$baseUrl/api/appointments/slots").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMyBookings(userId: String): List<AppointmentBookingApi> {
        return try {
            client.get("$baseUrl/api/appointments/my-bookings/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun bookAppointment(userId: String, slotId: Int, note: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/appointments/book") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("student_id" to userId, "slot_id" to slotId, "note" to note))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateAppointmentStatus(bookingId: Int, status: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/appointments/update_status") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("booking_id" to bookingId, "status" to status))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getStudentExams(userId: String): List<ExamSubmissionApi> {
 // Reusing ExamSubmissionApi for list
        return try {
            client.get("$baseUrl/api/student/exams/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getExamDetail(examId: Int): ExamDetailApi? {
        return try {
            client.get("$baseUrl/api/exam/$examId").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTranscript(userId: String): TranscriptApi? {
        return try {
            client.get("$baseUrl/api/student/transcript/$userId").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTeacherSubmissions(userId: String): List<AssignmentSubmissionApi> {
        return try {
            client.get("$baseUrl/api/teacher/submissions/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun gradeAssignmentSubmission(request: GradeSubmissionRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/teacher/submissions/grade") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createTeacherSlot(userId: String, date: String, start: String, end: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/teacher/slots/create") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("user_id" to userId, "date" to date, "start" to start, "end" to end))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteTeacherSlot(slotId: Int): Boolean {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/teacher/slots/delete/$slotId")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCourseStudents(courseId: Int): List<StudentShortApi> {
        return try {
            client.get("$baseUrl/api/course/students/$courseId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTeacherSchemes(userId: String): List<CourseAssessmentSchemeApi> {
        return try {
            client.get("$baseUrl/api/teacher/schemes/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateAssessmentScheme(scheme: CourseAssessmentSchemeApi): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/teacher/update_scheme") {
                contentType(ContentType.Application.Json)
                setBody(scheme)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTeacherSlots(userId: String): List<TeacherSlotApi> {
        return try {
            client.get("$baseUrl/api/teacher/slots/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTeacherBookings(userId: String): List<TeacherBookingApi> {
        return try {
            client.get("$baseUrl/api/teacher/bookings/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTeacherQuizSubmissions(userId: String): List<QuizSubmissionApi> {
        return try {
            client.get("$baseUrl/api/teacher/quiz_submissions/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTeacherExamSubmissions(userId: String): List<ExamSubmissionApi> {
        return try {
            client.get("$baseUrl/api/teacher/exam_submissions/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTeacherMeetings(userId: String): List<VClassMeetingApi> {
        return try {
            client.get("$baseUrl/api/teacher/meetings/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createMeeting(request: CreateMeetingRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/meetings/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTeacherQuizzes(userId: String): List<QuizDetailApi> {
        return try {
            client.get("$baseUrl/api/quiz/teacher/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createQuiz(request: QuizCreateRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/quiz/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateQuiz(quizId: Int, request: QuizCreateRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/quiz/update/$quizId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteQuiz(quizId: Int): Boolean {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/quiz/delete/$quizId")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun submitQuiz(userId: String, quizId: Int, answers: Map<String, String>): QuizSubmissionResponse? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/quiz/submit") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("student_id" to userId, "quiz_id" to quizId, "answers" to answers))
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun gradeQuiz(submissionId: Int, score: Float): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/quiz/grade") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("submission_id" to submissionId, "score" to score))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getClassPerformance(courseId: Int): List<ClassPerformanceApi> {
        return try {
            client.get("$baseUrl/api/teacher/performance/$courseId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAvailableRegistration(userId: String): List<CourseRegistrationApi> {
        return try {
            client.get("$baseUrl/api/student/registration/available/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun registerCourses(request: RegisterCoursesRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/student/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun payFees(request: PayFeesRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/student/fees/pay") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFeeTransactions(userId: String): List<FeeTransactionApi> {
        return try {
            client.get("$baseUrl/api/student/fees/transactions/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTeacherExams(userId: String): List<ExamSubmissionApi> {
 // Reusing for list overview
        return try {
            client.get("$baseUrl/api/exams/teacher/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun uploadMaterial(request: UploadMaterialRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/materials/upload") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadMaterialMultipart(
        title: String,
        programme: String,
        level: String,
        courseName: String,
        files: List<Pair<String, ByteArray>>
    ): Boolean {
        return try {
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "$baseUrl/api/materials/upload",
                formData = formData {
                    append("title", title)
                    append("programme", programme)
                    append("level", level)
                    append("course_name", courseName)
                    files.forEach { (name, content) ->
                        append("files", content, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                        })
                    }
                }
            )
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTeacherVClassAssignments(userId: String): List<VClassAssignmentApi> {
        return try {
            client.get("$baseUrl/api/teacher/vclass/assignments/$userId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createExam(request: ExamCreateRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/exams/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteExam(examId: Int): Boolean {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/exams/delete/$examId")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAttendance(request: MarkAttendanceRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/attendance/mark") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAttendanceByDate(courseId: Int, date: String): List<AttendanceRecordApi> {
        return try {
            client.get("$baseUrl/api/attendance/course/$courseId/date/$date").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAttendanceAnalytics(courseId: Int): List<AttendanceAnalyticsApi> {
        return try {
            client.get("$baseUrl/api/attendance/analytics/$courseId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteAttendanceByDate(courseId: Int, date: String): Boolean {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/attendance/course/$courseId/date/$date")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    // --- Paystack Integration ---
    suspend fun initializePaystack(userId: String, amount: Double, email: String): PaystackInitializeResponse? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/finance/paystack/initialize") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("user_id" to userId, "amount" to amount.toString(), "email" to email))
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<PaystackInitializeResponse>()
            } else {
                val errorMsg = response.bodyAsText()
                PaystackInitializeResponse(status = false, message = errorMsg, data = null)
            }
        } catch (e: Exception) {
            PaystackInitializeResponse(status = false, message = e.message ?: "Connection Error", data = null)
        }
    }

    suspend fun verifyPaystack(reference: String): PaystackVerifyResponse? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/finance/paystack/verify/$reference")
            if (response.status == HttpStatusCode.OK) {
                response.body<PaystackVerifyResponse>()
            } else {
                val errorMsg = response.bodyAsText()
                PaystackVerifyResponse(status = false, message = errorMsg, data = null)
            }
        } catch (e: Exception) {
            PaystackVerifyResponse(status = false, message = e.message ?: "Connection Error", data = null)
        }
    }

    suspend fun getChatHistory(receiverId: String): List<ChatMessageApi> {
        return try {
            client.get("$baseUrl/api/chat/history/$receiverId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getWhiteboardRoom(meetingId: Int): WhiteboardRoomResponse? {
        return try {
            client.get("$baseUrl/api/vclass/whiteboard/$meetingId").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLiveKitToken(roomName: String, userId: String, userName: String, role: String = "audience"): LiveKitTokenResponse? {
        return try {
            val staticUrl = "https://vtiu-lms-production-770d.up.railway.app"
            val response: HttpResponse = client.post("$staticUrl/api/livekit/token") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "roomName" to roomName,
                    "identity" to userId,
                    "name" to userName,
                    "role" to role
                ))
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<LiveKitTokenResponse>()
            } else {
                null
            }
        } catch (e: Exception) {
            println("LiveKit Token Error: ${e.message}")
            null
        }
    }

    suspend fun getAgoraToken(channelName: String, userId: String, role: String = "audience"): AgoraTokenResponse? {
        return try {
            // SYNC: Call Flask (Static URL) for tokens as requested by the live broadcasting setup
            val staticUrl = "https://vtiu-lms-production-770d.up.railway.app"
            val response: HttpResponse = client.post("$staticUrl/api/agora/token") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "channelName" to channelName,
                    "uid" to userId,
                    "role" to role
                ))
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<AgoraTokenResponse>()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Agora Token Error: ${e.message}")
            null
        }
    }
}
