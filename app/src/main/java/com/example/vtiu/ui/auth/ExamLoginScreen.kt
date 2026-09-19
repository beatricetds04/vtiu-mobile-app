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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vtiu.ui.teacher.TeacherViewModel
import com.example.vtiu.R

@Composable
fun ExamLoginScreen(
    onLoginSuccess: (String, String, String?, Int) -> Unit,
    onBackClick: () -> Unit,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
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

    val examGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image with Dark Overlay
        AsyncImage(
            model = "https://i.postimg.cc/x1nWRzPh/exam-background.jpg",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.7f)
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo Section
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
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Exams Login",
                        style = TextStyle(
                            brush = examGradient,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Enter your credentials to access your exams",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                    )

                    if (loginState?.success == false) {
                        Text(text = loginState?.message ?: "Login failed", color = Color.Red, fontSize = 12.sp)
                    }

                    // Username Field
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Username", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your Username") },
                            shape = RoundedCornerShape(28.dp),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF6A11CB)) },
                            singleLine = true,
                            enabled = !isLoading
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User ID Field
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Student ID", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = userId,
                            onValueChange = { userId = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your Student ID") },
                            shape = RoundedCornerShape(28.dp),
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF6A11CB)) },
                            singleLine = true,
                            enabled = !isLoading
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    var passwordVisible by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Password", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your password") },
                            shape = RoundedCornerShape(28.dp),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6A11CB)) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF6A11CB)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !isLoading
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Login Button
                    Button(
                        onClick = { viewModel.login(username, userId, password, "student") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        enabled = !isLoading && username.isNotBlank() && userId.isNotBlank() && password.isNotBlank()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(examGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onBackClick) {
                        Text("Back to Selection", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
