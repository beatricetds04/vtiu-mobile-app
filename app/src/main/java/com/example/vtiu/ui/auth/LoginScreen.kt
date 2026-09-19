package com.example.vtiu.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vtiu.ui.teacher.TeacherViewModel
import com.example.vtiu.ui.theme.SchoolPrimary
import com.example.vtiu.R

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String, String?, Int) -> Unit,
    viewModel: TeacherViewModel = hiltViewModel() // We can reuse the same VM for now or create a separate one later
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

    val schoolGreen = Color(0xFF00C950)

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        AsyncImage(
            model = "https://i.postimg.cc/wvy0XwYx/studying.png",
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Student Portal",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            
            Text(
                text = "Sign in to continue to your dashboard",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Login Card
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
                    Column {
                        Text(
                            text = "Student login",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter your details below to access your account.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

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
                        Text(text = "Username", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = { Text("Enter your username") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50.dp),
                            enabled = !isLoading
                        )
                    }

                    // Student ID Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Student ID", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        OutlinedTextField(
                            value = userId,
                            onValueChange = { userId = it },
                            placeholder = { Text("e.g. STD001") },
                            leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50.dp),
                            enabled = !isLoading
                        )
                    }

                    // Password Field
                    var passwordVisible by remember { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Password", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Enter your password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50.dp),
                            enabled = !isLoading
                        )
                    }

                    Button(
                        onClick = { viewModel.login(username, userId, password, "student") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = schoolGreen),
                        enabled = !isLoading && username.isNotBlank() && userId.isNotBlank() && password.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Text(
                        text = "Need help signing in? Contact your campus administrator.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Secure student portal",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
