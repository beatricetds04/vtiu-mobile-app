package com.example.vtiu.ui.teacher

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
class TeacherViewModel @Inject constructor(
    private val repository: LmsRepository
) : ViewModel() {

    private val _syncStatus = mutableStateOf("Not Started")
    val syncStatus: State<String> = _syncStatus

    private val _loginState = mutableStateOf<LoginResponse?>(null)
    val loginState: State<LoginResponse?> = _loginState

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _teacherClasses = mutableStateOf<List<TeacherClassApi>>(emptyList())
    val teacherClasses: State<List<TeacherClassApi>> = _teacherClasses

    private val _calendarEvents = mutableStateOf<List<CalendarEventApi>>(emptyList())
    val calendarEvents: State<List<CalendarEventApi>> = _calendarEvents

    private val _timetable = mutableStateOf<List<TimetableEntryApi>>(emptyList())
    val timetable: State<List<TimetableEntryApi>> = _timetable

    private val _classPerformance = mutableStateOf<List<ClassPerformanceApi>>(emptyList())
    val classPerformance: State<List<ClassPerformanceApi>> = _classPerformance

    private val _courseStudents = mutableStateOf<List<StudentShortApi>>(emptyList())
    val courseStudents: State<List<StudentShortApi>> = _courseStudents

    private val _assignmentSubmissions = mutableStateOf<List<AssignmentSubmissionApi>>(emptyList())
    val assignmentSubmissions: State<List<AssignmentSubmissionApi>> = _assignmentSubmissions

    private val _teacherSlots = mutableStateOf<List<TeacherSlotApi>>(emptyList())
    val teacherSlots: State<List<TeacherSlotApi>> = _teacherSlots

    private val _teacherBookings = mutableStateOf<List<TeacherBookingApi>>(emptyList())
    val teacherBookings: State<List<TeacherBookingApi>> = _teacherBookings

    private val _assessmentSchemes = mutableStateOf<List<CourseAssessmentSchemeApi>>(emptyList())
    val assessmentSchemes: State<List<CourseAssessmentSchemeApi>> = _assessmentSchemes

    private val _teacherMeetings = mutableStateOf<List<VClassMeetingApi>>(emptyList())
    val teacherMeetings: State<List<VClassMeetingApi>> = _teacherMeetings

    private val _quizSubmissions = mutableStateOf<List<QuizSubmissionApi>>(emptyList())
    val quizSubmissions: State<List<QuizSubmissionApi>> = _quizSubmissions

    private val _examSubmissions = mutableStateOf<List<ExamSubmissionApi>>(emptyList())
    val examSubmissions: State<List<ExamSubmissionApi>> = _examSubmissions

    private val _vclassAssignments = mutableStateOf<List<VClassAssignmentApi>>(emptyList())
    val vclassAssignments: State<List<VClassAssignmentApi>> = _vclassAssignments

    private val _teacherMaterials = mutableStateOf<List<MaterialApi>>(emptyList())
    val teacherMaterials: State<List<MaterialApi>> = _teacherMaterials

    private val _profile = mutableStateOf<UserProfileData?>(null)
    val profile: State<UserProfileData?> = _profile

    private val _attendanceByDate = mutableStateOf<List<AttendanceRecordApi>>(emptyList())
    val attendanceByDate: State<List<AttendanceRecordApi>> = _attendanceByDate

    private val _attendanceAnalytics = mutableStateOf<List<AttendanceAnalyticsApi>>(emptyList())
    val attendanceAnalytics: State<List<AttendanceAnalyticsApi>> = _attendanceAnalytics

    private val _teacherQuizzes = mutableStateOf<List<QuizDetailApi>>(emptyList())
    val teacherQuizzes: State<List<QuizDetailApi>> = _teacherQuizzes

    private val _teacherExams = mutableStateOf<List<ExamSubmissionApi>>(emptyList())
    val teacherExams: State<List<ExamSubmissionApi>> = _teacherExams

    private val _quizDetail = mutableStateOf<QuizDetailApi?>(null)
    val quizDetail: State<QuizDetailApi?> = _quizDetail

    private val _agoraToken = mutableStateOf<AgoraTokenResponse?>(null)
    val agoraToken: State<AgoraTokenResponse?> = _agoraToken

    fun loadTeacherClasses(userId: String) {
        viewModelScope.launch {
            _profile.value = repository.getProfile("teacher", userId).profile
            _teacherClasses.value = repository.getTeacherClasses(userId)
            _timetable.value = repository.getTeacherTimetable(userId)
            _assignmentSubmissions.value = repository.getTeacherSubmissions(userId)
            _teacherSlots.value = repository.getTeacherSlots(userId)
            _teacherBookings.value = repository.getTeacherBookings(userId)
            _assessmentSchemes.value = repository.getTeacherSchemes(userId)
            _teacherMeetings.value = repository.getTeacherMeetings(userId)
            _quizSubmissions.value = repository.getTeacherQuizSubmissions(userId)
            _examSubmissions.value = repository.getTeacherExamSubmissions(userId)
            _teacherQuizzes.value = repository.getTeacherQuizzes(userId)
            _teacherExams.value = repository.getTeacherExams(userId)
            _vclassAssignments.value = repository.getTeacherVClassAssignments(userId)
            _teacherMaterials.value = repository.getTeacherVClassMaterials(userId)
        }
    }

    fun loadPerformance(courseId: Int) {
        viewModelScope.launch {
            _classPerformance.value = repository.getClassPerformance(courseId)
            _courseStudents.value = repository.getCourseStudents(courseId)
        }
    }

    fun loadCalendar() {
        viewModelScope.launch {
            _calendarEvents.value = repository.getCalendar()
        }
    }

    fun loadTeacherQuizzes(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            _teacherQuizzes.value = repository.getTeacherQuizzes(userId)
            _isLoading.value = false
        }
    }

    fun updateAssessmentScheme(scheme: CourseAssessmentSchemeApi, userId: String) {
        viewModelScope.launch {
            val success = repository.updateAssessmentScheme(scheme)
            if (success) {
                // Reload schemes
                _assessmentSchemes.value = repository.getTeacherSchemes(userId)
            }
        }
    }

    fun login(username: String, userId: String, pass: String, role: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val response = repository.login(LoginRequest(username, userId, pass, role))
            _loginState.value = response
            _isLoading.value = false
        }
    }

    fun testFlaskConnection() {
        _syncStatus.value = "Connecting..."
        viewModelScope.launch {
            _syncStatus.value = repository.testConnection()
        }
    }

    fun createSlot(userId: String, date: String, start: String, end: String) {
        viewModelScope.launch {
            val success = repository.createTeacherSlot(userId, date, start, end)
            if (success) {
                _teacherSlots.value = repository.getTeacherSlots(userId)
            }
        }
    }

    fun deleteSlot(userId: String, slotId: Int) {
        viewModelScope.launch {
            val success = repository.deleteTeacherSlot(slotId)
            if (success) {
                _teacherSlots.value = repository.getTeacherSlots(userId)
            }
        }
    }

    fun updateBookingStatus(userId: String, bookingId: Int, status: String) {
        viewModelScope.launch {
            val success = repository.updateAppointmentStatus(bookingId, status)
            if (success) {
                _teacherBookings.value = repository.getTeacherBookings(userId)
            }
        }
    }

    fun loadAttendanceByDate(courseId: Int, date: String) {
        viewModelScope.launch {
            _attendanceByDate.value = repository.getAttendanceByDate(courseId, date)
        }
    }

    fun markAttendance(courseId: Int, teacherId: String, date: String, records: List<AttendanceRecordApi>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.markAttendance(MarkAttendanceRequest(courseId, teacherId, date, records))
            if (success) {
                onSuccess()
            }
        }
    }

    fun loadAttendanceAnalytics(courseId: Int) {
        viewModelScope.launch {
            _attendanceAnalytics.value = repository.getAttendanceAnalytics(courseId)
        }
    }

    fun deleteAttendanceByDate(courseId: Int, date: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteAttendanceByDate(courseId, date)
            if (success) {
                _attendanceByDate.value = emptyList()
                onSuccess()
            }
        }
    }

    fun createQuiz(request: QuizCreateRequest, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.createQuiz(request)
            if (success) {
                _teacherQuizzes.value = repository.getTeacherQuizzes(userId)
                onSuccess()
            }
        }
    }

    fun updateQuiz(quizId: Int, request: QuizCreateRequest, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.updateQuiz(quizId, request)
            if (success) {
                _teacherQuizzes.value = repository.getTeacherQuizzes(userId)
                onSuccess()
            }
        }
    }

    fun deleteQuiz(quizId: Int, userId: String) {
        viewModelScope.launch {
            val success = repository.deleteQuiz(quizId)
            if (success) {
                _teacherQuizzes.value = repository.getTeacherQuizzes(userId)
            }
        }
    }

    fun gradeQuiz(submissionId: Int, score: Float, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.gradeQuiz(submissionId, score)
            if (success) {
                _quizSubmissions.value = repository.getTeacherQuizSubmissions(userId)
                onSuccess()
            }
        }
    }

    fun loadQuizDetail(quizId: Int) {
        viewModelScope.launch {
            _quizDetail.value = repository.getQuizDetail(quizId)
        }
    }

    fun createExam(request: ExamCreateRequest, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.createExam(request)
            if (success) {
                _teacherExams.value = repository.getTeacherExams(userId)
                onSuccess()
            }
        }
    }

    fun deleteExam(examId: Int, userId: String) {
        viewModelScope.launch {
            val success = repository.deleteExam(examId)
            if (success) {
                _teacherExams.value = repository.getTeacherExams(userId)
            }
        }
    }

    fun gradeAssignmentSubmission(submissionId: Int, score: Float, feedback: String, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.gradeAssignmentSubmission(GradeSubmissionRequest(submissionId, score, feedback))
            if (success) {
                _assignmentSubmissions.value = repository.getTeacherSubmissions(userId)
                onSuccess()
            }
        }
    }

    fun uploadMaterial(request: UploadMaterialRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.uploadMaterial(request)
            if (success) {
                onSuccess()
            }
        }
    }

    fun uploadMaterialWithFiles(
        title: String,
        programme: String,
        level: String,
        courseName: String,
        files: List<Pair<String, ByteArray>>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.uploadMaterialMultipart(title, programme, level, courseName, files)
            if (success) {
                onSuccess()
            }
        }
    }

    fun deleteMaterial(materialId: Int, userId: String) {
        viewModelScope.launch {
            val success = repository.deleteMaterial(materialId)
            if (success) {
                _teacherMaterials.value = repository.getTeacherVClassMaterials(userId)
            }
        }
    }

    fun createMeeting(request: CreateMeetingRequest, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.createMeeting(request)
            if (success) {
                _teacherMeetings.value = repository.getTeacherMeetings(userId)
                onSuccess()
            }
        }
    }

    private val _liveKitToken = mutableStateOf<LiveKitTokenResponse?>(null)
    val liveKitToken: State<LiveKitTokenResponse?> = _liveKitToken

    fun loadLiveKitToken(roomName: String, userId: String, userName: String) {
        viewModelScope.launch {
            _liveKitToken.value = repository.getLiveKitToken(roomName, userId, userName, role = "publisher")
        }
    }
}
