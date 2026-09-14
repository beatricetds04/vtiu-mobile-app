package com.example.vtiu.data.model.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ProfileResponse(
    val success: Boolean,
    val profile: UserProfileData? = null,
    val message: String? = null
)

@Serializable
data class UserProfileData(
    @SerialName("user_id") val userId: String,
    val username: String,
    val name: String,
    val email: String?,
    val role: String,
    val department: String? = null,
    @SerialName("employee_id") val employeeId: String? = null,
    val qualification: String? = null,
    val specialization: String? = null,
    @SerialName("office_location") val officeLocation: String? = null,
    val programme: String? = null,
    val level: Int? = null,
    @SerialName("index_number") val indexNumber: String? = null,
    @SerialName("profile_picture_url") val profilePictureUrl: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val nationality: String? = null,
    val religion: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("academic_year") val academicYear: String? = null,
    val semester: String? = null,
    @SerialName("academic_status") val academicStatus: String? = null
)

@Serializable
data class QuizDetailApi(
    val id: Int,
    val title: String,
    @SerialName("course_name") val courseName: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("max_score") val maxScore: Float,
    @SerialName("start_datetime") val startDatetime: String,
    @SerialName("end_datetime") val endDatetime: String,
    @SerialName("attempts_allowed") val attemptsAllowed: Int,
    val questions: List<QuizQuestionApi> = emptyList()
)

@Serializable
data class QuizQuestionApi(
    val id: Int,
    @SerialName("text") val questionText: String,
    @SerialName("type") val questionType: String,
    val points: Float,
    val options: List<QuizOptionApi> = emptyList()
)

@Serializable
data class QuizOptionApi(
    val id: Int,
    val text: String,
    @SerialName("is_correct") val isCorrect: Boolean = false
)

@Serializable
data class StudentShortApi(
    val id: Int,
    @SerialName("user_id") val userId: String,
    val name: String
)

@Serializable
data class TeacherClassApi(
    val id: Int,
    @SerialName("course_name") val courseName: String,
    @SerialName("course_code") val courseCode: String,
    val programme: String,
    val level: String,
    @SerialName("student_count") val studentCount: Int = 0
)

@Serializable
data class StudentCourseApi(
    val id: Int,
    val name: String,
    val code: String,
    val level: String,
    val credits: Int
)

@Serializable
data class CalendarEventApi(
    val id: Int,
    val title: String,
    val date: String,
    val type: String,
    @SerialName("is_workday") val isWorkday: Boolean
)

@Serializable
data class MaterialApi(
    val id: Int,
    val title: String,
    @SerialName("course_name") val courseName: String? = null,
    val filename: String,
    @SerialName("file_type") val fileType: String,
    val date: String
)

@Serializable
data class VClassAssignmentApi(
    val id: Int,
    val title: String,
    @SerialName("course_name") val courseName: String? = null,
    @SerialName("due_date") val dueDate: String,
    val description: String? = null
)

@Serializable
data class VClassMeetingApi(
    val id: Int,
    val title: String,
    @SerialName("course_name") val courseName: String,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("host_id") val hostId: Int? = null,
    @SerialName("meeting_code") val meetingCode: String,
    val start: String,
    val end: String,
    @SerialName("is_live") val isLive: Boolean
)

@Serializable
data class JoinMeetingByCodeApi(
    @SerialName("app_id") val appId: String,
    val channel: String,
    val token: String,
    val uid: Int,
    val role: String,
    @SerialName("channel_profile") val channelProfile: String,
    @SerialName("meeting_id") val meetingId: Int,
    val title: String
)

@Serializable
data class FeeBalanceApi(
    @SerialName("amount_due") val amountDue: Double,
    @SerialName("amount_paid") val amountPaid: Double,
    val balance: Double
)

@Serializable
data class TimetableEntryApi(
    @SerialName("course_name") val courseName: String,
    @SerialName("course_code") val courseCode: String,
    val day: String,
    val start: String,
    val end: String,
    val venue: String? = null
)

@Serializable
data class StudentResultApi(
    @SerialName("course_name") val courseName: String,
    @SerialName("course_code") val courseCode: String? = null,
    val title: String,
    val score: Float,
    val max: Float,
    val date: String
)

@Serializable
data class NotificationApi(
    val id: Int,
    val title: String,
    val message: String,
    val type: String,
    val priority: String,
    val sender: String? = null,
    val date: String,
    @SerialName("is_read") val isRead: Boolean
)

@Serializable
data class ClassPerformanceApi(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String,
    @SerialName("quiz_score") val quizScore: Double,
    @SerialName("assignment_score") val assignmentScore: Double,
    @SerialName("exam_score") val examScore: Double,
    @SerialName("total_score") val totalScore: Double,
    val grade: String,
    val status: String
)

@Serializable
data class AppointmentSlotApi(
    val id: Int,
    val teacher: String,
    val date: String,
    val start: String,
    val end: String
)

@Serializable
data class AppointmentBookingApi(
    val id: Int,
    val teacher: String,
    val date: String,
    val start: String,
    val status: String,
    val note: String? = null
)

@Serializable
data class AssignmentSubmissionApi(
    val id: Int,
    @SerialName("student_name") val studentName: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("assignment_title") val assignmentTitle: String,
    @SerialName("submitted_at") val submittedAt: String,
    val status: String
)

@Serializable
data class QuizSubmissionApi(
    val id: Int,
    @SerialName("student_name") val studentName: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("quiz_title") val quizTitle: String,
    @SerialName("course_name") val courseName: String? = null,
    @SerialName("submitted_at") val submittedAt: String,
    val score: Float?,
    val max: Float
)

@Serializable
data class ExamSubmissionApi(
    val id: Int,
    @SerialName("student_name") val studentName: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("exam_title") val examTitle: String,
    @SerialName("submitted_at") val submittedAt: String,
    val score: Float?,
    val max: Float
)

@Serializable
data class ExamDetailApi(
    val id: Int,
    val title: String,
    @SerialName("course_name") val courseName: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("start_datetime") val startDatetime: String,
    @SerialName("end_datetime") val endDatetime: String,
    val questions: List<ExamQuestionApi>
)

@Serializable
data class ExamQuestionApi(
    val id: Int,
    val text: String,
    val type: String,
    val marks: Float,
    val options: List<ExamOptionApi>
)

@Serializable
data class ExamOptionApi(
    val id: Int,
    val text: String
)

@Serializable
data class TranscriptApi(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String,
    @SerialName("academic_year") val academicYear: String? = null,
    val semester: String? = null,
    @SerialName("is_released") val isReleased: Boolean = true,
    val gpa: Float,
    @SerialName("weighted_gpa") val weightedGpa: Float? = null,
    @SerialName("cumulative_gpa") val cumulativeGpa: Float? = null,
    @SerialName("total_credits") val totalCredits: Int,
    @SerialName("total_credits_attempted") val totalCreditsAttempted: Int? = null,
    @SerialName("total_credits_earned") val totalCreditsEarned: Int? = null,
    val courses: List<TranscriptCourseApi> = emptyList(),
    val semesters: List<TranscriptSemesterApi> = emptyList()
)

@Serializable
data class TranscriptSemesterApi(
    @SerialName("academic_year") val academicYear: String,
    val semester: String,
    val gpa: Float,
    @SerialName("is_released") val isReleased: Boolean = true,
    val courses: List<TranscriptCourseApi>
)

@Serializable
data class TranscriptCourseApi(
    val code: String,
    val name: String,
    val credits: Int,
    val score: Float? = null,
    val grade: String? = null,
    @SerialName("quiz_score") val quizScore: Float? = null,
    @SerialName("assignment_score") val assignmentScore: Float? = null,
    @SerialName("exam_score") val examScore: Float? = null
)

@Serializable
data class CourseAssessmentSchemeApi(
    val id: Int,
    @SerialName("course_id") val courseId: Int,
    @SerialName("course_name") val courseName: String,
    @SerialName("course_code") val courseCode: String,
    @SerialName("quiz_weight") val quizWeight: Float,
    @SerialName("assignment_weight") val assignmentWeight: Float,
    @SerialName("exam_weight") val examWeight: Float
)

@Serializable
data class TeacherSlotApi(
    val id: Int,
    val date: String,
    val start: String,
    val end: String,
    @SerialName("is_booked") val isBooked: Boolean
)

@Serializable
data class TeacherBookingApi(
    val id: Int,
    @SerialName("student_name") val studentName: String,
    @SerialName("student_id") val studentId: String,
    val date: String,
    val start: String,
    val status: String,
    val note: String? = null
)

@Serializable
data class AttendanceRecordApi(
    val id: Int? = null,
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("is_present") val isPresent: Boolean
)

@Serializable
data class MarkAttendanceRequest(
    @SerialName("course_id") val courseId: Int,
    @SerialName("teacher_id") val teacherId: String,
    val date: String,
    val records: List<AttendanceRecordApi>
)

@Serializable
data class AttendanceAnalyticsApi(
    @SerialName("student_name") val studentName: String,
    val total: Int,
    val present: Int,
    val absent: Int,
    val percentage: Float
)

@Serializable
data class QuizCreateRequest(
    val title: String,
    @SerialName("course_name") val courseName: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("start_datetime") val startDatetime: String,
    val questions: List<QuizQuestionCreateApi>
)

@Serializable
data class QuizQuestionCreateApi(
    val text: String,
    val type: String,
    val points: Float,
    val options: List<String>,
    @SerialName("correct_option_index") val correctOptionIndex: Int
)

@Serializable
data class QuizSubmissionResponse(
    val success: Boolean,
    val score: Float,
    val total: Float
)

@Serializable
data class CourseRegistrationApi(
    val id: Int,
    val name: String,
    val code: String,
    val level: String,
    val credits: Int,
    @SerialName("is_mandatory") val isMandatory: Boolean,
    val semester: String
)

@Serializable
data class RegisterCoursesRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("course_ids") val courseIds: List<Int>
)

@Serializable
data class PayFeesRequest(
    @SerialName("user_id") val userId: String,
    val amount: Double,
    val description: String
)

@Serializable
data class FeeTransactionApi(
    val id: Int,
    val date: String,
    val amount: Double,
    val description: String,
    @SerialName("is_approved") val isApproved: Boolean
)

@Serializable
data class ExamCreateRequest(
    val title: String,
    @SerialName("course_name") val courseName: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("start_datetime") val startDatetime: String,
    @SerialName("assignment_mode") val assignmentMode: String,
    val questions: List<ExamQuestionCreateApi>
)

@Serializable
data class ExamQuestionCreateApi(
    val text: String,
    val type: String,
    val marks: Int,
    val options: List<String>,
    @SerialName("correct_option_index") val correctOptionIndex: Int
)

@Serializable
data class UploadMaterialRequest(
    val title: String,
    val programme: String,
    val level: String,
    @SerialName("course_name") val courseName: String,
    val filenames: List<String>
)

@Serializable
data class CreateMeetingRequest(
    val title: String,
    @SerialName("host_user_id") val hostUserId: String,
    @SerialName("course_name") val courseName: String,
    val start: String,
    val end: String
)

@Serializable
data class StudentFullResultApi(
    @SerialName("course_name") val courseName: String,
    @SerialName("course_code") val courseCode: String,
    val score: Float,
    val max: Float,
    val grade: String,
    val credits: Int,
    val gp: Float,
    @SerialName("quiz_weight") val quizWeight: Float,
    @SerialName("assignment_weight") val assignmentWeight: Float,
    @SerialName("exam_weight") val examWeight: Float,
    val remark: String
)

@Serializable
data class GradeSubmissionRequest(
    @SerialName("submission_id") val submissionId: Int,
    val score: Float,
    val feedback: String
)

@Serializable
data class StudentAssessmentApi(
    val id: Int,
    val type: String,
    val course: String,
    val title: String,
    @SerialName("raw_score") val rawScore: Float,
    @SerialName("max_score") val maxScore: Float,
    val date: String,
    val feedback: String? = null
)

// --- Paystack Models ---
@Serializable
data class PaystackInitializeResponse(
    val status: Boolean,
    val message: String,
    val data: PaystackData? = null
)

@Serializable
data class PaystackData(
    @SerialName("authorization_url") val authorizationUrl: String,
    @SerialName("access_code") val accessCode: String,
    val reference: String
)

@Serializable
data class PaystackVerifyResponse(
    val status: Boolean,
    val message: String,
    val data: PaystackVerifyData? = null
)

@Serializable
data class PaystackVerifyData(
    val status: String,
    val reference: String,
    val amount: Long,
    val metadata: Map<String, String>? = null
)

// --- Chat Models ---
@Serializable
data class ChatMessageApi(
    val id: Int? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("receiver_id") val receiverId: String,
    val message: String,
    val timestamp: String? = null,
    @SerialName("is_read") val isRead: Boolean = false
)

@Serializable
data class WhiteboardRoomResponse(
    val type: String = "excalidraw",
    val roomUrl: String,
    val roomUuid: String = ""
)

@Serializable
data class AgoraTokenResponse(
    val token: String,
    val appId: String
)

@Serializable
data class LiveKitTokenResponse(
    val token: String,
    val serverUrl: String
)
