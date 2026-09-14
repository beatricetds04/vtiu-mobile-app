package com.example.vtiu.ui.vclass

import android.Manifest
import android.util.Log
import android.view.SurfaceView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vtiu.data.local.SessionManager
import com.example.vtiu.ui.dashboard.StudentViewModel
import com.example.vtiu.ui.chat.ChatViewModel
import com.example.vtiu.data.model.api.VClassMeetingApi
import com.example.vtiu.ui.theme.VClassPrimary
import kotlinx.coroutines.delay

import com.example.vtiu.data.remote.LiveKitManager
import io.livekit.android.renderer.TextureViewRenderer

@Composable
fun VClassLiveClassRoomScreen(
    meetingId: Int,
    userId: String,
    onLeaveClick: () -> Unit,
    viewModel: StudentViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    sessionManager: SessionManager
) {
    val context = LocalContext.current
    val liveKitManager = remember { LiveKitManager(context) }
    
    val studentMeetings by viewModel.vclassMeetings
    val meetingDetail by viewModel.meetingDetail
    
    val meeting = studentMeetings.find { it.id == meetingId } ?: meetingDetail ?: VClassMeetingApi(
        id = meetingId,
        title = "Loading...",
        courseName = "Course",
        teacherName = "Teacher",
        meetingCode = "",
        start = "",
        end = "",
        isLive = true
    )
    
    val liveKitTokenResponse by viewModel.liveKitToken
    val chatRoomId = "meeting_$meetingId"

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        // Handle permissions
    }

    LaunchedEffect(Unit) {
        viewModel.loadMeetingDetail(meetingId)
        viewModel.loadWhiteboardRoom(meetingId)
        chatViewModel.connect(userId, chatRoomId)
        
        // Request Audio permission
        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }

    LaunchedEffect(meeting.meetingCode, userId) {
        if (meeting.meetingCode.isNotEmpty() && userId.isNotEmpty()) {
            viewModel.loadLiveKitToken(meeting.meetingCode, userId, sessionManager.getUserName() ?: userId)
        }
    }

    LaunchedEffect(liveKitTokenResponse) {
        liveKitTokenResponse?.let {
            liveKitManager.joinRoom(it.serverUrl, it.token)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            liveKitManager.release()
            chatViewModel.disconnect()
        }
    }

    ActiveTeachingRoom(meeting, liveKitManager, userId, viewModel, chatViewModel, chatRoomId, onLeaveClick)
}

@Composable
fun WaitingRoom(teacherName: String, onLeaveClick: () -> Unit) {
    Scaffold(
        containerColor = Color(0xFF0F1720),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00C950), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Waiting for Host...",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Please stay on this screen. $teacherName will let you into the class shortly.",
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(64.dp))
            OutlinedButton(
                onClick = onLeaveClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.3f)))
            ) {
                Text("Leave Meeting")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTeachingRoom(
    meeting: VClassMeetingApi,
    liveKitManager: LiveKitManager,
    currentUserId: String,
    viewModel: StudentViewModel,
    chatViewModel: ChatViewModel,
    chatRoomId: String,
    onLeaveClick: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val messages = chatViewModel.messages
    var isFullScreen by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    
    val videoTracks = liveKitManager.videoTracks

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isFullScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onLeaveClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Leave",
                                    tint = Color.White
                                )
                            }
                            Column {
                                Text(meeting.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Live • ${meeting.teacherName}", fontSize = 12.sp, color = Color(0xFF00C950))
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { 
                                    isMuted = !isMuted
                                    // TODO: Implement mute in LiveKitManager
                                },
                                modifier = Modifier.size(32.dp).padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Toggle Mic",
                                    tint = if (isMuted) Color.Red else Color(0xFF00C950)
                                )
                            }

                            Button(
                                onClick = onLeaveClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(32.dp).padding(end = 8.dp)
                            ) {
                                Text("Leave", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isFullScreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f))
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoTracks.isNotEmpty()) {
                        AndroidView(
                            factory = { ctx ->
                                TextureViewRenderer(ctx).apply {
                                    // Initialize if needed (usually handled by SDK but manual init might be required)
                                }
                            },
                            update = { renderer ->
                                videoTracks.first().addRenderer(renderer)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VideocamOff, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                            Text(
                                text = "Waiting for teacher video...",
                                color = Color.White,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                    
                    Surface(
                        color = Color.Red.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Text(
                            text = "REC",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(Color.White)
                    ) {
                        Text(
                            text = "Live Class Chat",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages) { msg ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(VClassPrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = VClassPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = msg.senderName ?: "User", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = msg.message, fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        Surface(
                            shadowElevation = 12.dp,
                            tonalElevation = 2.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding()
                                .navigationBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFF1F3F4)),
                                    placeholder = { Text("Ask a question...", color = Color.Gray) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { 
                                        if (messageText.isNotBlank()) {
                                            chatViewModel.sendMessage(currentUserId, messageText, chatRoomId)
                                            messageText = ""
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = VClassPrimary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            if (isFullScreen) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
                    IconButton(
                        onClick = { isFullScreen = false },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                    }
                }
            }
        }
    }
}
