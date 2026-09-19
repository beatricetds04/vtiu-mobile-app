package com.example.vtiu.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vtiu.ui.teacher.TeacherViewModel
import com.example.vtiu.ui.theme.TeacherPrimary
import com.example.vtiu.R

@Composable
fun TeacherLoginScreen(
    onLoginSuccess: (String, String, String?, Int) -> Unit,
    onBackClick: () -> Unit,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var teacherId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val loginState by viewModel.loginState
    val isLoading by viewModel.isLoading

    LaunchedEffect(loginState) {
        loginState?.let {
            if (it.success && it.user != null) {
                onLoginSuccess(it.user.userId, it.user.name, it.user.profilePictureUrl, it.user.id)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        AsyncImage(
            model = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&q=80&w=1000",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Section
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.school_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Teacher Portal",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Log in with your Teacher ID and Password",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (loginState?.success == false) {
                        Text(
                            text = loginState?.message ?: "Login failed",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Username Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Username", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your username") },
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = TeacherPrimary) },
                            singleLine = true,
                            enabled = !isLoading
                        )
                    }

                    // Teacher ID Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Teacher ID", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = teacherId,
                            onValueChange = { teacherId = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. TCH001") },
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = TeacherPrimary) },
                            singleLine = true,
                            enabled = !isLoading
                        )
                    }

                    // Password Field
                    var passwordVisible by remember { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Password", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your password") },
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TeacherPrimary) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = TeacherPrimary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !isLoading
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Login Button
                    Button(
                        onClick = { viewModel.login(username, teacherId, password, "teacher") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TeacherPrimary),
                        enabled = !isLoading && username.isNotBlank() && teacherId.isNotBlank() && password.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onBackClick) {
                Text("Back to Portal Selection", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
