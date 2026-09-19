package com.example.vtiu

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.vtiu.data.local.SessionManager
import com.example.vtiu.ui.auth.ExamLoginScreen
import com.example.vtiu.ui.auth.LoginScreen
import com.example.vtiu.ui.auth.PortalSelectionScreen
import com.example.vtiu.ui.auth.TeacherLoginScreen
import com.example.vtiu.ui.assessments.AssessmentsScreen
import com.example.vtiu.ui.chat.ChatScreen
import com.example.vtiu.ui.calendar.AcademicCalendarScreen
import com.example.vtiu.ui.appointments.AppointmentBookingScreen
import com.example.vtiu.ui.appointments.MyAppointmentsScreen
import com.example.vtiu.ui.courses.CourseListScreen
import com.example.vtiu.ui.courses.CourseRegistrationScreen
import com.example.vtiu.ui.dashboard.DashboardScreen
import com.example.vtiu.ui.exams.ExamInstructionsScreen
import com.example.vtiu.ui.exams.ExamListScreen
import com.example.vtiu.ui.exams.ExamTakeScreen
import com.example.vtiu.ui.fees.FeesScreen
import com.example.vtiu.ui.fees.PayFeesScreen
import com.example.vtiu.ui.fees.PaystackPaymentScreen
import com.example.vtiu.ui.notifications.NotificationScreen
import com.example.vtiu.ui.navigation.Screen
import com.example.vtiu.ui.profile.IdCardScreen
import com.example.vtiu.ui.profile.ProfileScreen
import com.example.vtiu.ui.results.ResultsScreen
import com.example.vtiu.ui.results.TranscriptScreen
import com.example.vtiu.ui.timetable.TimetableScreen
import com.example.vtiu.ui.teacher.*
import com.example.vtiu.ui.vclass.*
import com.example.vtiu.ui.theme.SchoolPrimary
import com.example.vtiu.ui.theme.VTIUTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VTIUTheme {
                VtiuApp(sessionManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VtiuApp(sessionManager: SessionManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val startDestination = if (sessionManager.isLoggedIn()) {
        if (sessionManager.getUserRole() == "teacher") Screen.TeacherDashboard.route 
        else Screen.Dashboard.route
    } else {
        Screen.PortalSelection.route
    }

    val showDrawer = when {
        currentDestination?.route == null -> false
        currentDestination.route == Screen.PortalSelection.route -> false
        currentDestination.route!!.contains("login") -> false
        currentDestination.route!!.startsWith("live_class_room") -> false
        currentDestination.route!!.startsWith("vclass_pdf_viewer") -> false
        currentDestination.route!!.startsWith("take_quiz") -> false
        currentDestination.route!!.startsWith("take_exam") -> false
        else -> true
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer,
        drawerContent = {
            if (showDrawer) {
                AppDrawer(
                    userName = sessionManager.getUserName() ?: "User",
                    userId = sessionManager.getUserId() ?: "ID",
                    userRole = sessionManager.getUserRole() ?: "student",
                    profilePic = sessionManager.getProfilePic(),
                    currentRoute = currentDestination?.route,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        if (route == "logout") {
                            sessionManager.logout()
                            navController.navigate(Screen.PortalSelection.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val hostPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = 0.dp
            )

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(hostPadding)
            ) {
                composable(Screen.PortalSelection.route) {
                    PortalSelectionScreen(
                        onStudentClick = { navController.navigate(Screen.StudentLogin.route) },
                        onTeacherClick = { navController.navigate(Screen.TeacherLogin.route) },
                        onVClassClick = { navController.navigate(Screen.StudentLogin.route) },
                        onExamClick = { navController.navigate(Screen.ExamLogin.route) }
                    )
                }
                composable(Screen.StudentLogin.route) {
                    LoginScreen(
                        onLoginSuccess = { userId, userName, profilePic, numericId ->
                            sessionManager.saveSession(userId, "student", userName, profilePic, numericId)
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.PortalSelection.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.TeacherLogin.route) {
                    TeacherLoginScreen(
                        onLoginSuccess = { userId, userName, profilePic, numericId ->
                            sessionManager.saveSession(userId, "teacher", userName, profilePic, numericId)
                            navController.navigate(Screen.TeacherDashboard.route) {
                                popUpTo(Screen.PortalSelection.route) { inclusive = true }
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.ExamLogin.route) {
                    ExamLoginScreen(
                        onLoginSuccess = { userId, userName, profilePic, numericId ->
                            sessionManager.saveSession(userId, "student", userName, profilePic, numericId)
                            navController.navigate(Screen.Exams.route) {
                                popUpTo(Screen.PortalSelection.route) { inclusive = true }
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        onTileClick = { title ->
                            when (title) {
                                "My Courses" -> navController.navigate(Screen.Courses.route)
                                "My Profile" -> navController.navigate(Screen.Profile.route)
                                "Registration" -> navController.navigate(Screen.CourseRegistration.route)
                                "Results" -> navController.navigate(Screen.Results.route)
                                "Assessments" -> navController.navigate(Screen.Assessments.route)
                                "Appointments" -> navController.navigate(Screen.BookAppointment.route)
                                "Academic Calendar" -> navController.navigate(Screen.AcademicCalendar.route)
                                "Transcript" -> navController.navigate(Screen.Transcript.route)
                                "Exams" -> navController.navigate(Screen.Exams.route)
                                "Timetable" -> navController.navigate(Screen.Timetable.route)
                                "Fees" -> navController.navigate(Screen.Fees.route)
                                "Virtual Class" -> navController.navigate(Screen.VClassDashboard.route)
                                "Global Chat" -> navController.navigate(Screen.Chat.route)
                            }
                        },
                        onNotificationClick = {
                            navController.navigate(Screen.Notifications.route)
                        },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherDashboard.route) {
                    TeacherDashboardScreen(
                        onActionClick = { route ->
                            when (route) {
                                "attendance" -> navController.navigate(Screen.TeacherClasses.route)
                                "submissions" -> navController.navigate(Screen.TeacherGrading.route)
                                else -> navController.navigate(route)
                            }
                        },
                        onLogoutClick = {
                            sessionManager.logout()
                            navController.navigate(Screen.PortalSelection.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherClasses.route) {
                    TeacherClassesScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onClassClick = { courseId -> 
                            navController.navigate(Screen.TeacherAttendanceHub.createRoute(courseId))
                        },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherProfile.route) {
                    TeacherProfileScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onLogoutClick = {
                            sessionManager.logout()
                            navController.navigate(Screen.PortalSelection.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        sessionManager = sessionManager
                    )
                }
                composable(
                    Screen.TeacherAttendanceHub.route,
                    arguments = listOf(navArgument("courseId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val courseId = backStackEntry.arguments?.getInt("courseId") ?: 0
                    TeacherAttendanceHubScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onMarkAttendanceClick = { navController.navigate(Screen.TeacherAttendance.createRoute(courseId)) },
                        onViewAnalyticsClick = { navController.navigate(Screen.TeacherAttendanceAnalytics.createRoute(courseId)) }
                    )
                }
                composable(
                    Screen.TeacherAttendance.route,
                    arguments = listOf(navArgument("courseId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val courseId = backStackEntry.arguments?.getInt("courseId") ?: 0
                    TeacherAttendanceScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        courseId = courseId,
                        sessionManager = sessionManager
                    )
                }
                composable(
                    Screen.TeacherAttendanceAnalytics.route,
                    arguments = listOf(navArgument("courseId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val courseId = backStackEntry.arguments?.getInt("courseId") ?: 0
                    TeacherAttendanceAnalyticsScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        courseId = courseId,
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherGrading.route) {
                    TeacherGradeSubmissionsScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherAssessmentScheme.route) {
                    TeacherAssessmentSchemeScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherClassPerformance.route) {
                    TeacherClassPerformanceScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherAssignmentFileManager.route) {
                    TeacherAssignmentFileManagerScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherAppointmentHub.route) {
                    TeacherAppointmentHubScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onManageSlotsClick = { navController.navigate(Screen.TeacherManageSlots.route) },
                        onViewRequestsClick = { navController.navigate(Screen.TeacherAppointmentRequests.route) }
                    )
                }
                composable(Screen.TeacherManageSlots.route) {
                    TeacherManageSlotsScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherAppointmentRequests.route) {
                    TeacherAppointmentRequestsScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherQuizHub.route) {
                    TeacherQuizHubScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onCreateQuizClick = { navController.navigate(Screen.TeacherCreateQuiz.createRoute()) },
                        onManageQuizzesClick = { navController.navigate(Screen.TeacherManageQuizzes.route) },
                        onViewSubmissionsClick = { navController.navigate(Screen.TeacherQuizSubmissions.route) }
                    )
                }
                composable(Screen.TeacherManageQuizzes.route) {
                    TeacherManageQuizzesScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onEditQuizClick = { quizId -> 
                            navController.navigate(Screen.TeacherCreateQuiz.createRoute(quizId))
                        },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherQuizSubmissions.route) {
                    TeacherQuizSubmissionsScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(
                    route = Screen.TeacherCreateQuiz.route,
                    arguments = listOf(navArgument("quizId") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val quizIdStr = backStackEntry.arguments?.getString("quizId")
                    val quizId = quizIdStr?.toIntOrNull()
                    TeacherQuizCreatorScreen(
                        quizId = quizId,
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onQuizPublished = {
                            navController.popBackStack(Screen.TeacherQuizHub.route, false)
                        },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherExamHub.route) {
                    TeacherExamHubScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onCreateExamClick = { navController.navigate(Screen.TeacherCreateExam.route) },
                        onManageExamsClick = { navController.navigate(Screen.TeacherManageExams.route) },
                        onViewSubmissionsClick = { navController.navigate(Screen.TeacherExamSubmissions.route) }
                    )
                }
                composable(Screen.TeacherManageExams.route) {
                    TeacherManageExamsScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onEditExamClick = { /* Edit */ },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherExamSubmissions.route) {
                    TeacherExamSubmissionsScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherLiveHub.route) {
                    TeacherLiveHubScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onCreateLiveClick = { navController.navigate(Screen.TeacherCreateLive.route) },
                        onHostClick = { id -> navController.navigate(Screen.TeacherLiveRoom.createRoute(id)) },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherCreateLive.route) {
                    TeacherCreateLiveScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onScheduleSuccess = {
                            navController.popBackStack(Screen.TeacherLiveHub.route, false)
                        },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherLiveRoom.route, arguments = listOf(navArgument("meetingId") { type = NavType.IntType })) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("meetingId") ?: 0
                    TeacherLiveRoomScreen(
                        meetingId = id, 
                        onEndClick = { navController.popBackStack() },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherMaterialsHub.route) {
                    TeacherMaterialsHubScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onUploadClick = { navController.navigate(Screen.TeacherMaterialsUploader.route) },
                        onManageClick = { navController.navigate(Screen.TeacherManageMaterials.route) }
                    )
                }
                composable(Screen.TeacherManageMaterials.route) {
                    TeacherManageMaterialsScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherMaterialsUploader.route) {
                    TeacherMaterialsUploaderScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onUploadSuccess = {
                            navController.popBackStack(Screen.TeacherDashboard.route, false)
                        },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.TeacherCreateExam.route) {
                    TeacherExamCreatorScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onExamPublished = {
                            navController.popBackStack(Screen.TeacherExamHub.route, false)
                        },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.Courses.route) {
                    CourseListScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.CourseRegistration.route) {
                    CourseRegistrationScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.Results.route) {
                    ResultsScreen(
                        onBackClick = { navController.popBackStack() },
                        onViewTranscriptClick = { navController.navigate(Screen.Transcript.route) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.Transcript.route) {
                    TranscriptScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.Chat.route) {
                    ChatScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.Assessments.route) {
                    AssessmentsScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.AcademicCalendar.route) {
                    AcademicCalendarScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.BookAppointment.route) {
                    AppointmentBookingScreen(
                        onBackClick = { navController.popBackStack() },
                        onViewMyAppointments = { navController.navigate(Screen.MyAppointments.route) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.MyAppointments.route) {
                    MyAppointmentsScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.Exams.route) {
                    ExamListScreen(
                        onBackClick = { navController.popBackStack() },
                        onExamClick = { id -> navController.navigate(Screen.ExamInstructions.createRoute(id)) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(
                    route = Screen.ExamInstructions.route,
                    arguments = listOf(navArgument("examId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("examId") ?: 0
                    ExamInstructionsScreen(
                        examId = id,
                        onBackClick = { navController.popBackStack() },
                        onStartExamClick = { examId -> navController.navigate(Screen.TakeExam.createRoute(examId)) },
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
                composable(
                    route = Screen.TakeExam.route,
                    arguments = listOf(navArgument("examId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("examId") ?: 0
                    ExamTakeScreen(
                        examId = id,
                        onBackClick = { navController.popBackStack() },
                        onSubmitSuccess = { title, score, total ->
                            navController.navigate(Screen.QuizResult.createRoute(title, score, total)) {
                                popUpTo(Screen.Exams.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Timetable.route) {
                    TimetableScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.Fees.route) {
                    FeesScreen(
                        onBackClick = { navController.popBackStack() },
                        onPayNowClick = { navController.navigate(Screen.PayFees.route) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.PayFees.route) {
                    PayFeesScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onInitiatePaystack = { url, ref ->
                            navController.navigate(Screen.PaystackCheckout.createRoute(url, ref))
                        },
                        sessionManager = sessionManager
                    )
                }
                composable(
                    route = Screen.PaystackCheckout.route,
                    arguments = listOf(
                        navArgument("url") { type = NavType.StringType },
                        navArgument("reference") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val url = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
                    val ref = backStackEntry.arguments?.getString("reference") ?: ""
                    PaystackPaymentScreen(
                        url = url,
                        reference = ref,
                        onBackClick = { navController.popBackStack() },
                        onSuccess = {
                            navController.popBackStack(Screen.Fees.route, false)
                        }
                    )
                }
                composable(Screen.Notifications.route) {
                    NotificationScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.VClassDashboard.route) {
                    VClassDashboardScreen(
                        onBackClick = { navController.popBackStack() },
                        onMaterialsClick = { navController.navigate(Screen.VClassMaterials.route) },
                        onAssignmentsClick = { navController.navigate(Screen.VClassAssignments.route) },
                        onLiveClick = { navController.navigate(Screen.VClassLive.route) },
                        onQuizClick = { quizId -> navController.navigate(Screen.QuizInstructions.createRoute(quizId)) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(
                    route = Screen.QuizInstructions.route,
                    arguments = listOf(navArgument("quizId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val quizId = backStackEntry.arguments?.getInt("quizId") ?: 0
                    VClassQuizInstructionsScreen(
                        quizId = quizId,
                        onBackClick = { navController.popBackStack() },
                        onStartQuizClick = { id -> navController.navigate(Screen.TakeQuiz.createRoute(id)) },
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
                composable(
                    route = Screen.TakeQuiz.route,
                    arguments = listOf(navArgument("quizId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val quizId = backStackEntry.arguments?.getInt("quizId") ?: 0
                    VClassTakeQuizScreen(
                        quizId = quizId,
                        onBackClick = { navController.popBackStack() },
                        onSubmitSuccess = { title, score, total ->
                            navController.navigate(Screen.QuizResult.createRoute(title, score, total)) {
                                popUpTo(Screen.VClassDashboard.route)
                            }
                        },
                        sessionManager = sessionManager
                    )
                }
                composable(
                    route = Screen.QuizResult.route,
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType },
                        navArgument("score") { type = NavType.FloatType },
                        navArgument("total") { type = NavType.FloatType }
                    )
                ) { backStackEntry ->
                    val title = backStackEntry.arguments?.getString("title") ?: "Quiz"
                    val score = backStackEntry.arguments?.getFloat("score") ?: 0f
                    val total = backStackEntry.arguments?.getFloat("total") ?: 0f
                    VClassQuizResultScreen(
                        quizTitle = title,
                        score = score,
                        total = total,
                        onBackToDashboard = {
                            navController.popBackStack(Screen.Dashboard.route, false)
                        }
                    )
                }
                composable(Screen.VClassMaterials.route) {
                    VClassMaterialsScreen(
                        onBackClick = { navController.popBackStack() },
                        onPreviewClick = { id -> navController.navigate(Screen.VClassPdfViewer.createRoute(id)) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(
                    route = Screen.VClassPdfViewer.route,
                    arguments = listOf(navArgument("materialId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("materialId") ?: 0
                    VClassPdfViewerScreen(
                        materialId = id,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.VClassLive.route) {
                    VClassLiveScreen(
                        onBackClick = { navController.popBackStack() },
                        onJoinClick = { id -> navController.navigate(Screen.JoinMeeting.createRoute(id)) },
                        onRecordingClick = { id -> navController.navigate(Screen.VClassPlayer.createRoute(id)) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(
                    route = Screen.VClassPlayer.route,
                    arguments = listOf(navArgument("recordingId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("recordingId") ?: 0
                    VClassVideoPlayerScreen(
                        recordingId = id,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.JoinMeeting.route,
                    arguments = listOf(navArgument("meetingId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("meetingId") ?: 0
                    VClassJoinMeetingScreen(
                        meetingId = id,
                        onBackClick = { navController.popBackStack() },
                        onJoinNowClick = { meetingId ->
                            navController.navigate(Screen.LiveClassRoom.createRoute(meetingId))
                        }
                    )
                }
                composable(
                    route = Screen.LiveClassRoom.route,
                    arguments = listOf(navArgument("meetingId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("meetingId") ?: 0
                    VClassLiveClassRoomScreen(
                        meetingId = id,
                        userId = sessionManager.getUserId() ?: "",
                        onLeaveClick = { navController.popBackStack() },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.VClassAssignments.route) {
                    VClassAssignmentsScreen(
                        onBackClick = { navController.popBackStack() },
                        onSubmitClick = { id -> navController.navigate(Screen.SubmitAssignment.createRoute(id)) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(
                    route = Screen.SubmitAssignment.route,
                    arguments = listOf(navArgument("assignmentId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("assignmentId") ?: 0
                    VClassSubmitAssignmentScreen(
                        assignmentId = id,
                        onBackClick = { navController.popBackStack() },
                        onSubmitSuccess = {
                            navController.popBackStack()
                        },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onBackClick = { navController.popBackStack() },
                        onIdCardClick = { navController.navigate(Screen.IdCard.route) },
                        onLogoutClick = {
                            sessionManager.logout()
                            navController.navigate(Screen.PortalSelection.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
                composable(Screen.IdCard.route) {
                    IdCardScreen(
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        sessionManager = sessionManager
                    )
                }
            }
        }
    }
}

@Composable
fun AppDrawer(
    userName: String,
    userId: String,
    userRole: String,
    profilePic: String?,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        drawerContentColor = Color.Black,
        modifier = Modifier.width(300.dp)
    ) {
        // Drawer Header
        DrawerHeader(userName, userId, userRole, profilePic)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (userRole == "teacher") {
                // --- TEACHER MENUS ---
                DrawerItem(label = "Home", icon = Icons.Default.Home, selected = currentRoute == Screen.TeacherDashboard.route, onClick = { onNavigate(Screen.TeacherDashboard.route) })
                DrawerItem(label = "My Classes", icon = Icons.Default.Groups, selected = currentRoute == Screen.TeacherClasses.route, onClick = { onNavigate(Screen.TeacherClasses.route) })
                DrawerItem(label = "Attendance", icon = Icons.Default.HowToReg, selected = false, onClick = { onNavigate(Screen.TeacherClasses.route) })
                DrawerItem(label = "Live Classes", icon = Icons.Default.VideoCall, selected = currentRoute == Screen.TeacherLiveHub.route, onClick = { onNavigate(Screen.TeacherLiveHub.route) })
                DrawerItem(label = "Appointments", icon = Icons.Default.CalendarMonth, selected = currentRoute == Screen.TeacherAppointmentHub.route, onClick = { onNavigate(Screen.TeacherAppointmentHub.route) })
                DrawerItem(label = "Quizzes", icon = Icons.Default.Quiz, selected = currentRoute == Screen.TeacherQuizHub.route, onClick = { onNavigate(Screen.TeacherQuizHub.route) })
                DrawerItem(label = "Exams", icon = Icons.Default.Description, selected = currentRoute == Screen.TeacherExamHub.route, onClick = { onNavigate(Screen.TeacherExamHub.route) })
                DrawerItem(label = "Assignments", icon = Icons.Default.FolderZip, selected = currentRoute == Screen.TeacherAssignmentFileManager.route, onClick = { onNavigate(Screen.TeacherAssignmentFileManager.route) })
                DrawerItem(label = "Materials", icon = Icons.Default.FolderOpen, selected = currentRoute == Screen.TeacherMaterialsHub.route, onClick = { onNavigate(Screen.TeacherMaterialsHub.route) })
                DrawerItem(label = "Class Results", icon = Icons.Default.Analytics, selected = currentRoute == Screen.TeacherClassPerformance.route, onClick = { onNavigate(Screen.TeacherClassPerformance.route) })
            } else {
                // --- STUDENT MENUS ---
                // Section 1: Main
                DrawerItem(label = "Home", icon = Icons.Default.Home, selected = currentRoute == Screen.Dashboard.route, onClick = { onNavigate(Screen.Dashboard.route) })
                DrawerItem(label = "Courses", icon = Icons.Default.AutoStories, selected = currentRoute == Screen.Courses.route, onClick = { onNavigate(Screen.Courses.route) })
                DrawerItem(label = "Chat", icon = Icons.Default.ChatBubbleOutline, selected = currentRoute == Screen.Chat.route, onClick = { onNavigate(Screen.Chat.route) })
                DrawerItem(label = "Profile", icon = Icons.Default.PersonOutline, selected = currentRoute == Screen.Profile.route, onClick = { onNavigate(Screen.Profile.route) })
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))
                
                // Section 2: Features
                DrawerItem(label = "Results", icon = Icons.AutoMirrored.Filled.ShowChart, selected = currentRoute == Screen.Results.route, onClick = { onNavigate(Screen.Results.route) })
                DrawerItem(label = "Assessments", icon = Icons.Default.AssignmentTurnedIn, selected = currentRoute == Screen.Assessments.route, onClick = { onNavigate(Screen.Assessments.route) })
                DrawerItem(label = "Exams", icon = Icons.Default.Description, selected = currentRoute == Screen.Exams.route, onClick = { onNavigate(Screen.Exams.route) })
                DrawerItem(label = "Timetable", icon = Icons.Default.CalendarMonth, selected = currentRoute == Screen.Timetable.route, onClick = { onNavigate(Screen.Timetable.route) })
                DrawerItem(label = "Fees", icon = Icons.Default.Payments, selected = currentRoute == Screen.Fees.route, onClick = { onNavigate(Screen.Fees.route) })
                DrawerItem(label = "Virtual Class", icon = Icons.Default.Laptop, selected = currentRoute == Screen.VClassDashboard.route, onClick = { onNavigate(Screen.VClassDashboard.route) })
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))
            
            // Section 3: Shared
            DrawerItem(label = "Settings", icon = Icons.Default.Settings, selected = false, onClick = { /* TODO */ })
            DrawerItem(label = "Log Out", icon = Icons.AutoMirrored.Filled.Logout, selected = false, color = Color.Red, onClick = { onNavigate("logout") })
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DrawerHeader(name: String, id: String, role: String, profilePic: String?) {
    val staticUrl = "https://vtiu-lms-production-770d.up.railway.app"
    val firstName = name.split(" ").firstOrNull() ?: name
    val idLabel = if (role == "teacher") "Employee ID" else "Student ID"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!profilePic.isNullOrBlank()) {
                AsyncImage(
                    model = "$staticUrl$profilePic",
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.school_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = firstName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "$idLabel: $id",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    color: Color = if (selected) SchoolPrimary else Color.Black.copy(alpha = 0.7f),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = if (selected) Color(0xFFE8F5E9) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = color
            )
        }
    }
}
