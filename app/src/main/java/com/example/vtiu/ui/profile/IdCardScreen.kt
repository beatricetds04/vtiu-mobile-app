package com.example.vtiu.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vtiu.ui.dashboard.StudentViewModel
import com.example.vtiu.ui.theme.SchoolPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdCardScreen(
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: StudentViewModel = hiltViewModel(),
    sessionManager: com.example.vtiu.data.local.SessionManager
) {
    val profileApi by viewModel.profile
    val userId = sessionManager.getUserId() ?: ""
    val context = LocalContext.current

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.loadStudentData(userId)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val profile = profileApi
        if (profile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SchoolPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .background(Color(0xFFF4F6F8))
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                        Text(
                            text = "Student ID Card",
                            fontSize = 22.sp,
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = "Tap to view back side",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        FlippableIdCard(profile)
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val idCardUrl = "https://vtiu-lms-production-770d.up.railway.app/static/uploads/id_cards/id_card_${userId}.pdf"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(idCardUrl))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Could not open PDF. Please install a PDF viewer.")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2C4D))
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Official ID", fontSize = 14.sp)
                            }
                        }
                    }

                    item {
                        CardInfoSection(profile)
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FlippableIdCard(profile: com.example.vtiu.data.model.api.UserProfileData) {
    var rotated by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "cardFlip"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.58f)
            .clickable { rotated = !rotated }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
    ) {
        if (rotation <= 90f) {
            IdCardFront(profile)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = 180f
                    }
            ) {
                IdCardBack(profile)
            }
        }
    }
}

@Composable
fun IdCardFront(profile: com.example.vtiu.data.model.api.UserProfileData) {
    val cardBg = Color(0xFF0D2C4D)
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            // Top Section: Logo and Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // School Logo
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = com.example.vtiu.R.drawable.school_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "VTIU COLLEGE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Student Details Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = profile.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "INDEX: ${profile.indexNumber ?: "3223820"}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End
                )
                Text(
                    text = profile.programme ?: "Cyber Security",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "ISSUED: JAN 2025 EXPIRES: JAN 2026",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "STUDENT ID CARD",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun IdCardBack(profile: com.example.vtiu.data.model.api.UserProfileData) {
    val headerBg = Color(0xFF0D2C4D)
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Blue Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VTIU COLLEGE - STUDENT CARD",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Property Text
                Text(
                    text = "This card remains the property of VTIU College.\nReturn it to the College if found or when requested.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 16.dp)
                )

                // Mock QR Code Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f))
                            .padding(4.dp)
                    ) {
                        // Mock QR pattern
                        Column(modifier = Modifier.fillMaxSize()) {
                            repeat(6) { r ->
                                Row(modifier = Modifier.weight(1f)) {
                                    repeat(6) { c ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(if ((r + c) % 3 == 0 || (r * c) % 4 == 0) Color.Black else Color.White)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardInfoSection(profile: com.example.vtiu.data.model.api.UserProfileData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Card Management", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            InfoRow("Student ID", profile.userId)
            InfoRow("Status", "Authorized Digital Copy")
            InfoRow("Cycle", "2025 Academic Year")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
