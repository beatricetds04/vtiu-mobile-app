package com.example.vtiu.ui.teacher

import android.Manifest
import android.os.Build
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vtiu.data.local.SessionManager
import com.example.vtiu.data.model.api.VClassMeetingApi
import com.example.vtiu.data.remote.AgoraManager
import com.example.vtiu.ui.chat.ChatViewModel
import com.example.vtiu.ui.theme.TeacherPrimary
import io.agora.rtc2.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherLiveRoomScreen(
    meetingId: Int,
    onEndClick: () -> Unit,
    viewModel: TeacherViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    sessionManager: SessionManager
) {
    val context = LocalContext.current
    val userId = sessionManager.getUserId() ?: ""
    val numericId = sessionManager.getNumericId()
    
    val teacherMeetings by viewModel.teacherMeetings
    val profile by viewModel.profile
    val students by viewModel.courseStudents
    val agoraTokenResponse by viewModel.agoraToken
    
    val meeting = teacherMeetings.find { it.id == meetingId } ?: VClassMeetingApi(
        id = meetingId, 
        title = "Loading...", 
        courseName = "", 
        teacherName = profile?.name ?: "Teacher", 
        meetingCode = "",
        start = "", 
        end = "", 
        isLive = true
    )
    
    // Agora Setup
    val agoraManager = remember { AgoraManager(context) }
    var hasPermissions by remember { mutableStateOf(false) }
    var isStreaming by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermissions = perms[Manifest.permission.CAMERA] == true &&
                         perms[Manifest.permission.RECORD_AUDIO] == true
    }

    // Auto-join with NO publishing for preview mode
    LaunchedEffect(agoraTokenResponse, hasPermissions) {
        if (agoraTokenResponse != null && hasPermissions && meeting.meetingCode.isNotEmpty()) {
            agoraManager.init(agoraTokenResponse!!.appId)
            agoraManager.startPreview()
            // Use only first 6 alphanumeric chars for the Agora channel
            val channelId = meeting.meetingCode.filter { it.isLetterOrDigit() }.take(6).uppercase()
            agoraManager.joinChannel(
                channelName = channelId,
                uid = numericId,
                token = agoraTokenResponse?.token?.ifEmpty { null },
                role = Constants.CLIENT_ROLE_BROADCASTER,
                publishCamera = false,
                publishMic = false
            )
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.loadTeacherClasses(userId)
        }
    }

    LaunchedEffect(meeting.id) {
        if (meeting.id != 0 && meeting.meetingCode.isNotEmpty()) {
            // Load Agora Token using the truncated and normalized channel ID
            val channelId = meeting.meetingCode.filter { it.isLetterOrDigit() }.take(6).uppercase()
            viewModel.loadAgoraToken(channelId, numericId.toString())
        }
    }

    LaunchedEffect(meeting.courseName) {
        if (meeting.courseName.isNotEmpty()) {
            val course = viewModel.teacherClasses.value.find { it.courseName == meeting.courseName }
            course?.let {
                viewModel.loadPerformance(it.id)
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
        
        // Connect Chat
        chatViewModel.connect(userId, "meeting_$meetingId")
    }

    DisposableEffect(Unit) {
        onDispose {
            agoraManager.stopPreview()
            agoraManager.leaveChannel()
            agoraManager.release()
            chatViewModel.disconnect()
        }
    }

    var messageText by remember { mutableStateOf("") }
    val messages = chatViewModel.messages
    
    // States for Controls
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOn by remember { mutableStateOf(true) }
    var isSharing by remember { mutableStateOf(false) }
    var showParticipants by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    
    // Convert StudentShortApi to Participant
    val participants = remember { mutableStateListOf<Participant>() }
    
    LaunchedEffect(students, profile) {
        participants.clear()
        participants.add(Participant("${profile?.name ?: "Teacher"} (You)", false))
        participants.addAll(students.map { Participant(it.name, false) })
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(meeting.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                text = if (isStreaming) "HOSTING • LIVE" else "READY TO GO", 
                                fontSize = 12.sp, 
                                color = if (isStreaming) Color(0xFF00C950) else Color.Yellow
                            )
                        }
                    },
                    actions = {
                        // Participant Counter - Now Clickable
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { if (isStreaming) showParticipants = true }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.People, contentDescription = "Participants", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isStreaming) participants.size.toString() else "0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        if (!isStreaming) {
                            Button(
                                onClick = {
                                    if (hasPermissions && agoraTokenResponse != null) {
                                        agoraManager.updatePublishState(publishCamera = true, publishMic = true)
                                        isStreaming = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C950)),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(32.dp).padding(end = 8.dp),
                                enabled = meeting.meetingCode.isNotEmpty() && agoraTokenResponse != null
                            ) {
                                Text("Start Live", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onEndClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(32.dp).padding(end = 8.dp)
                            ) {
                                Text("End", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onEndClick) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Content Area (Camera / Screen Share)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isFullScreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f))
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasPermissions && isCameraOn) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceView(ctx)
                            },
                            update = { view ->
                                agoraManager.setupLocalVideo(view)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isSharing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PresentToAll, 
                                contentDescription = null, 
                                tint = Color(0xFF2D8CFF), 
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("You are sharing your screen", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    } else if (!hasPermissions) {
                        Text("Permissions required for video", color = Color.White)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff, 
                                contentDescription = null, 
                                tint = Color.White.copy(alpha = 0.5f), 
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Camera is off", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                    }

                    // Recording Status
                    if (isStreaming) {
                        Surface(
                            color = Color.Red,
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("REC", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    // Full Screen Toggle
                    IconButton(
                        onClick = { isFullScreen = !isFullScreen },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Fullscreen",
                            tint = Color.White
                        )
                    }
                }

                if (!isFullScreen) {
                    // Controls & Chat
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(Color.White)
                    ) {
                        // Host Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ControlItem(
                                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = if (isMuted) "Unmute" else "Mute",
                                isActive = !isMuted,
                                onClick = { 
                                    isMuted = !isMuted
                                    agoraManager.muteLocalAudio(isMuted)
                                }
                            )
                            ControlItem(
                                icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                label = "Camera",
                                isActive = isCameraOn,
                                onClick = { 
                                    isCameraOn = !isCameraOn
                                    if (!isSharing) {
                                        agoraManager.muteLocalVideo(!isCameraOn)
                                    }
                                }
                            )
                            ControlItem(
                                icon = if (isSharing) Icons.AutoMirrored.Filled.StopScreenShare else Icons.AutoMirrored.Filled.ScreenShare,
                                label = if (isSharing) "Stop" else "Share",
                                isActive = isSharing,
                                onClick = { 
                                    isSharing = !isSharing
                                    if (isSharing) {
                                        agoraManager.startScreenSharing()
                                    } else {
                                        agoraManager.stopScreenSharing(isCameraOn)
                                    }
                                }
                            )
                            ControlItem(
                                icon = Icons.Default.Settings,
                                label = "Settings",
                                onClick = { /* Open settings */ }
                            )
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        Text(
                            text = "Live Chat",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages) { msg ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(TeacherPrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = TeacherPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = msg.senderName ?: "User", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TeacherPrimary)
                                        Text(text = msg.message, fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        // Chat Input
                        Surface(
                            shadowElevation = 8.dp,
                            modifier = Modifier.imePadding().navigationBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Reply to students...") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                                IconButton(onClick = { 
                                    if (messageText.isNotBlank()) {
                                        chatViewModel.sendMessage(userId, messageText, "meeting_$meetingId")
                                        messageText = ""
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = TeacherPrimary)
                                }
                            }
                        }
                    }
                }
            }
            
            // Overlays for Full Screen Mode
            if (isFullScreen) {
                // Minimize Button Overlay
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
                    IconButton(
                        onClick = { isFullScreen = false },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                    }
                }
                
                // Small Control Overlay at Bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            isMuted = !isMuted
                            agoraManager.muteLocalAudio(isMuted)
                        }) {
                            Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, null, tint = if (isMuted) Color.Red else Color.White)
                        }
                        IconButton(onClick = { 
                            isCameraOn = !isCameraOn
                            agoraManager.muteLocalVideo(!isCameraOn)
                        }) {
                            Icon(if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff, null, tint = if (isCameraOn) Color.White else Color.Red)
                        }
                        if (isStreaming) {
                            Text(text = "${participants.size} Students", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Participants Bottom Sheet
    if (showParticipants) {
        ModalBottomSheet(
            onDismissRequest = { showParticipants = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Participants (${participants.size})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { 
                            participants.indices.forEach { i -> 
                                if (!participants[i].name.contains("(You)")) {
                                    participants[i] = participants[i].copy(isMuted = true)
                                }
                            }
                        }) {
                            Text("Mute All", color = Color.Red, fontSize = 12.sp)
                        }
                        TextButton(onClick = { 
                            participants.indices.forEach { i -> 
                                participants[i] = participants[i].copy(isMuted = false)
                            }
                        }) {
                            Text("Unmute All", color = Color(0xFF43A047), fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(participants) { index, participant ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = participant.name,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(onClick = { 
                                if (!participant.name.contains("(You)")) {
                                    participants[index] = participant.copy(isMuted = !participant.isMuted)
                                } else {
                                    isMuted = !isMuted
                                    participants[index] = participant.copy(isMuted = isMuted)
                                }
                            }) {
                                Icon(
                                    imageVector = if (participant.isMuted) Icons.Default.MicOff else Icons.Default.Mic, 
                                    contentDescription = if (participant.isMuted) "Unmute" else "Mute", 
                                    tint = if (participant.isMuted) Color.Red else Color.Gray, 
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

data class Participant(val name: String, val isMuted: Boolean)

@Composable
fun ControlItem(
    icon: ImageVector, 
    label: String, 
    isActive: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = if (isActive) Color(0xFFF4F6F8) else Color.Red.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(44.dp),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon, 
                    contentDescription = label, 
                    tint = if (isActive) Color.DarkGray else Color.Red, 
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(text = label, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}
