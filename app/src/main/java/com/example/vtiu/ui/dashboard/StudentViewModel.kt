package com.example.vtiu.ui.dashboard

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vtiu.data.model.api.*
import com.example.vtiu.data.repository.LmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val repository: LmsRepository
) : ViewModel() {

    private val _courses = mutableStateOf<List<StudentCourseApi>>(emptyList())
    val courses: State<List<StudentCourseApi>> = _courses

    private val _feeBalance = mutableStateOf<FeeBalanceApi?>(null)
    val feeBalance: State<FeeBalanceApi?> = _feeBalance

    private val _materials = mutableStateOf<List<MaterialApi>>(emptyList())
    val materials: State<List<MaterialApi>> = _materials

    private val _vclassAssignments = mutableStateOf<List<VClassAssignmentApi>>(emptyList())
    val vclassAssignments: State<List<VClassAssignmentApi>> = _vclassAssignments

    private val _vclassMeetings = mutableStateOf<List<VClassMeetingApi>>(emptyList())
    val vclassMeetings: State<List<VClassMeetingApi>> = _vclassMeetings

    private val _timetable = mutableStateOf<List<TimetableEntryApi>>(emptyList())
    val timetable: State<List<TimetableEntryApi>> = _timetable

    private val _results = mutableStateOf<List<StudentResultApi>>(emptyList())
    val results: State<List<StudentResultApi>> = _results

    private val _notifications = mutableStateOf<List<NotificationApi>>(emptyList())
    val notifications: State<List<NotificationApi>> = _notifications

    private val _bookings = mutableStateOf<List<AppointmentBookingApi>>(emptyList())
    val bookings: State<List<AppointmentBookingApi>> = _bookings

    private val _transcript = mutableStateOf<TranscriptApi?>(null)
    val transcript: State<TranscriptApi?> = _transcript

    private val _availableSlots = mutableStateOf<List<AppointmentSlotApi>>(emptyList())
    val availableSlots: State<List<AppointmentSlotApi>> = _availableSlots

    private val _exams = mutableStateOf<List<ExamSubmissionApi>>(emptyList())
    val exams: State<List<ExamSubmissionApi>> = _exams

    private val _profile = mutableStateOf<UserProfileData?>(null)
    val profile: State<UserProfileData?> = _profile

    private val _quizDetail = mutableStateOf<QuizDetailApi?>(null)
    val quizDetail: State<QuizDetailApi?> = _quizDetail

    private val _examDetail = mutableStateOf<ExamDetailApi?>(null)
    val examDetail: State<ExamDetailApi?> = _examDetail

    private val _availableQuizzes = mutableStateOf<List<QuizDetailApi>>(emptyList())
    val availableQuizzes: State<List<QuizDetailApi>> = _availableQuizzes

    private val _availableRegistration = mutableStateOf<List<CourseRegistrationApi>>(emptyList())
    val availableRegistration: State<List<CourseRegistrationApi>> = _availableRegistration

    private val _fullResults = mutableStateOf<List<StudentFullResultApi>>(emptyList())
    val fullResults: State<List<StudentFullResultApi>> = _fullResults

    private val _studentAssessments = mutableStateOf<List<StudentAssessmentApi>>(emptyList())
    val studentAssessments: State<List<StudentAssessmentApi>> = _studentAssessments

    private val _feeTransactions = mutableStateOf<List<FeeTransactionApi>>(emptyList())
    val feeTransactions: State<List<FeeTransactionApi>> = _feeTransactions

    private val _whiteboardRoom = mutableStateOf<WhiteboardRoomResponse?>(null)
    val whiteboardRoom: State<WhiteboardRoomResponse?> = _whiteboardRoom

    private val _meetingDetail = mutableStateOf<VClassMeetingApi?>(null)
    val meetingDetail: State<VClassMeetingApi?> = _meetingDetail

    private val _agoraToken = mutableStateOf<AgoraTokenResponse?>(null)
    val agoraToken: State<AgoraTokenResponse?> = _agoraToken

    fun loadStudentData(userId: String) {
        viewModelScope.launch {
            val response = repository.getProfile("student", userId)
            _profile.value = response.profile
            _courses.value = repository.getStudentCourses(userId)
            _feeBalance.value = repository.getFeeBalance(userId)
            _timetable.value = repository.getStudentTimetable(userId)
            _results.value = repository.getStudentResults(userId)
            _fullResults.value = repository.getStudentResultsFull(userId)
            _studentAssessments.value = repository.getStudentAssessments(userId)
            _feeTransactions.value = repository.getFeeTransactions(userId)
            _notifications.value = repository.getNotifications(userId)
            _bookings.value = repository.getMyBookings(userId)
            _transcript.value = repository.getTranscript(userId)
            _availableSlots.value = repository.getAppointmentSlots()
            _exams.value = repository.getStudentExams(userId)
            _availableQuizzes.value = repository.getStudentQuizzes(userId)
            _vclassAssignments.value = repository.getStudentVClassAssignments(userId)
            _materials.value = repository.getStudentVClassMaterials(userId)
            _vclassMeetings.value = repository.getStudentVClassMeetings(userId)
        }
    }

    fun loadMaterials(courseId: Int) {
        viewModelScope.launch {
            _materials.value = repository.getMaterials(courseId)
            _vclassAssignments.value = repository.getVClassAssignments(courseId)
            _vclassMeetings.value = repository.getVClassMeetings(courseId)
        }
    }

    fun loadQuizDetail(quizId: Int) {
        viewModelScope.launch {
            _quizDetail.value = repository.getQuizDetail(quizId)
        }
    }

    fun loadExamDetail(examId: Int) {
        viewModelScope.launch {
            _examDetail.value = repository.getExamDetail(examId)
        }
    }

    fun bookAppointment(userId: String, slotId: Int, note: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.bookAppointment(userId, slotId, note)
            if (success) {
                onSuccess()
                // Refresh data
                _availableSlots.value = repository.getAppointmentSlots()
                _bookings.value = repository.getMyBookings(userId)
            }
        }
    }

    fun cancelAppointment(userId: String, bookingId: Int) {
        viewModelScope.launch {
            val success = repository.updateAppointmentStatus(bookingId, "cancelled")
            if (success) {
                _bookings.value = repository.getMyBookings(userId)
                _availableSlots.value = repository.getAppointmentSlots()
            }
        }
    }

    fun submitQuiz(userId: String, quizId: Int, answers: Map<String, String>, onSuccess: (Float, Float) -> Unit) {
        viewModelScope.launch {
            val response = repository.submitQuiz(userId, quizId, answers)
            if (response != null && response.success) {
                onSuccess(response.score, response.total)
            }
        }
    }

    fun loadAvailableRegistration(userId: String) {
        viewModelScope.launch {
            _availableRegistration.value = repository.getAvailableRegistration(userId)
        }
    }

    fun registerCourses(userId: String, courseIds: List<Int>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.registerCourses(RegisterCoursesRequest(userId, courseIds))
            if (success) {
                onSuccess()
                // Refresh courses
                _courses.value = repository.getStudentCourses(userId)
            }
        }
    }

    fun payFees(userId: String, amount: Double, description: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.payFees(PayFeesRequest(userId, amount, description))
            if (success) {
                onSuccess()
                _feeBalance.value = repository.getFeeBalance(userId)
            }
        }
    }

    fun submitAssignment(userId: String, assignmentId: Int, filename: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.submitAssignment(userId, assignmentId, filename)
            if (success) {
                onSuccess()
            }
        }
    }

    // --- Paystack Integration ---
    fun initializePayment(userId: String, amount: Double, email: String, onUrlReady: (String, String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.initializePaystack(userId, amount, email)
                if (response != null && response.status && response.data != null) {
                    onUrlReady(response.data.authorizationUrl, response.data.reference)
                } else {
                    onFailure("Initialization failed: ${response?.message ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                onFailure(e.message ?: "Connection error")
            }
        }
    }

    fun verifyPayment(reference: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val response = repository.verifyPaystack(reference)
            if (response != null && response.data?.status == "success") {
                onSuccess()
                // Refresh balance
                _profile.value?.userId?.let { uid ->
                    _feeBalance.value = repository.getFeeBalance(uid)
                    _feeTransactions.value = repository.getFeeTransactions(uid)
                }
            }
        }
    }

    fun loadWhiteboardRoom(meetingId: Int) {
        viewModelScope.launch {
            _whiteboardRoom.value = repository.getWhiteboardRoom(meetingId)
        }
    }

    fun loadMeetingDetail(meetingId: Int) {
        viewModelScope.launch {
            println("VClass: Loading meeting detail for $meetingId")
            val detail = repository.getMeetingDetail(meetingId)
            println("VClass: Detail received: ${detail?.title}")
            _meetingDetail.value = detail
        }
    }

    fun loadAgoraToken(channelName: String, userId: String) {
        viewModelScope.launch {
            _agoraToken.value = repository.getAgoraToken(channelName, userId, role = "audience")
        }
    }
}
