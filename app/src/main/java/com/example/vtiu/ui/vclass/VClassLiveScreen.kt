package com.example.vtiu.ui.vclass

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vtiu.ui.dashboard.StudentViewModel
import com.example.vtiu.data.model.VClassMeeting
import com.example.vtiu.data.model.VClassRecording
import com.example.vtiu.ui.theme.VClassPrimary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VClassLiveScreen(
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    onJoinClick: (Int) -> Unit,
    onRecordingClick: (Int) -> Unit,
    viewModel: StudentViewModel = hiltViewModel(),
    sessionManager: com.example.vtiu.data.local.SessionManager
) {
    val meetingsApi by viewModel.vclassMeetings
    val userId = sessionManager.getUserId() ?: ""
    
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.loadStudentData(userId)
        }
    }

    val meetings = meetingsApi.map { 
        VClassMeeting(it.id, it.title, it.courseName, it.teacherName ?: "Teacher", it.start, it.end, null, it.isLive, false)
    }
    val recordings = emptyList<VClassRecording>()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .background(Color(0xFFF4F4F4))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                    Text(
                        text = "Live Classes",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                IconButton(onClick = onMenuClick) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scheduled Sessions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "Auto-updates", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                items(meetings) { meeting ->
                    EnhancedMeetingCard(
                        meeting = meeting,
                        onJoinClick = { 
                            println("VClass: Joining Meeting ${meeting.id} (Title: ${meeting.title}, Course: ${meeting.courseName})")
                            onJoinClick(meeting.id) 
                        }
                    )
                }

                if (meetings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.LightGray.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No active or upcoming classes found for your programme.",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Past Recordings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                items(recordings) { recording ->
                    RecordingItem(
                        title = recording.title,
                        date = recording.recordedAt,
                        onPlayClick = { onRecordingClick(recording.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedMeetingCard(
    meeting: VClassMeeting,
    onJoinClick: () -> Unit
) {
    val now = Calendar.getInstance().time
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val startTime = sdf.parse(meeting.scheduledStart) ?: Date()
    val endTime = sdf.parse(meeting.scheduledEnd) ?: Date()
    
    val isUpcoming = now.before(startTime)
    val isLive = meeting.isLive || (now.after(startTime) && now.before(endTime))

    // Pulse Animation for LIVE badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Course Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isLive) Color.Red else VClassPrimary)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = meeting.courseName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (isLive) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                alpha = alpha
                            )
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Teacher Avatar
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(VClassPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = meeting.teacherName.take(1),
                            color = VClassPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        // SYNC: Show Meeting Title in bold, matches Web
                        Text(
                            text = meeting.title.uppercase(), 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        Text(text = meeting.teacherName, fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${meeting.scheduledStart.substring(11, 16)} – ${meeting.scheduledEnd.substring(11, 16)}", 
                        fontSize = 13.sp, 
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = meeting.scheduledStart.substring(0, 10), 
                        fontSize = 12.sp, 
                        color = Color.Gray
                    )
                }

                if (isUpcoming) {
                    CountdownTimer(startTime)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onJoinClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isLive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLive) Color(0xFF2D8CFF) else Color.LightGray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            isLive -> "Join Live Class"
                            isUpcoming -> "Not Started Yet"
                            else -> "Session Ended"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CountdownTimer(startTime: Date) {
    var timeLeft by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance().time
            val diff = startTime.time - now.time
            if (diff <= 0) {
                timeLeft = "Starting now!"
                break
            }
            val h = diff / 3600000
            val m = (diff % 3600000) / 60000
            val s = (diff % 60000) / 1000
            timeLeft = "Starts in ${h}h ${m}m ${s}s"
            delay(1000)
        }
    }

    Text(
        text = timeLeft,
        color = Color(0xFFF57F17),
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun RecordingItem(title: String, date: String, onPlayClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F6F8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VClassPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "Recorded on $date", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
