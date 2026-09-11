package com.example.myapplication.ui.dashboard

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.myapplication.ui.screening.ScreeningActivity
import com.example.myapplication.ui.theme.*

// --- Custom 3D Bounce Click Modifier ---
@Composable
fun Modifier.bounceClick(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onNavigateToHistory: () -> Unit) {
    val context = LocalContext.current
    val roleManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            context.getSystemService(RoleManager::class.java) else null
    }
    var isCallScreeningEnabled by remember { mutableStateOf(false) }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) isCallScreeningEnabled = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null)
            isCallScreeningEnabled = roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FB)) // Rich Light Background
    ) {
        // Glowing background blobs for attractive light theme
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .blur(140.dp)
                .background(Color(0xFFD0D7FF), CircleShape) // Soft blue glow
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .blur(140.dp)
                .background(Color(0xFFE1BEE7).copy(alpha = 0.6f), CircleShape) // Soft purple glow
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ─── Header ───────────────────────────────────────────────────────
            item {
                DashboardHeader(isEnabled = isCallScreeningEnabled)
            }

            // ─── Status Card ──────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                StatusCard(
                    isEnabled = isCallScreeningEnabled,
                    onClick = {
                        if (!isCallScreeningEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_CONTACTS,
                                    android.Manifest.permission.WRITE_CONTACTS,
                                    android.Manifest.permission.READ_CALL_LOG,
                                    android.Manifest.permission.WRITE_CALL_LOG,
                                    android.Manifest.permission.RECORD_AUDIO
                                )
                            )
                        }
                    }
                )
            }

            // ─── Quick Stats ──────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                QuickStatsRow()
            }

            // ─── Sandbox ──────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                ScreeningSandbox(onScreenNumber = { number ->
                    context.startActivity(
                        Intent(context, ScreeningActivity::class.java).apply {
                            putExtra(ScreeningActivity.EXTRA_PHONE_NUMBER, number)
                            putExtra("EXTRA_IS_SANDBOX", true)
                        }
                    )
                })
            }

            // ─── Quick Actions ────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.History,
                        title = "Call Log",
                        subtitle = "Past calls",
                        color = PrimaryIndigo,
                        onClick = onNavigateToHistory
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.BugReport,
                        title = "Test AI",
                        subtitle = "Simulate call",
                        color = SecondaryPurple,
                        onClick = {
                            context.startActivity(
                                Intent(context, ScreeningActivity::class.java).apply {
                                    putExtra(ScreeningActivity.EXTRA_PHONE_NUMBER, "+91 88776 65544")
                                    putExtra("EXTRA_IS_SANDBOX", true)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

// ─── Dashboard Header ─────────────────────────────────────────────────────────
@Composable
fun DashboardHeader(isEnabled: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryIndigo, SecondaryPurple)
                            )
                        )
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Assistant,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Nova Assistant",
                        color = Color(0xFF1E1E2C),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personal Call Manager",
                        color = Color(0xFF6E6E82),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ─── 3D Floating Status Card ───────────────────────────────────────────────────
@Composable
fun StatusCard(isEnabled: Boolean, onClick: () -> Unit) {
    // Floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "floatY"
    )

    val bgColor = if (isEnabled) Brush.linearGradient(listOf(Color(0xFF5C6BC0), Color(0xFF7E57C2))) 
                  else Brush.linearGradient(listOf(Color(0xFF9E9E9E), Color(0xFF757575)))
    val icon = if (isEnabled) Icons.Rounded.Shield else Icons.Rounded.GppBad
    val title = if (isEnabled) "Assistant Active" else "Setup Required"
    val subtitle = if (isEnabled) "Nova is screening your incoming calls." else "Tap to set as default dialer."

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .offset(y = floatY.dp) // 3D floating effect
            .bounceClick(onClick = onClick) // 3D click effect
            .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = PrimaryIndigo.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(28.dp))
            .background(bgColor)
            .padding(24.dp)
    ) {
        // Shine overlay effect
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color.White.copy(alpha=0.15f), Color.Transparent))
        ))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ─── Quick Stats Row ──────────────────────────────────────────────────────────
@Composable
fun QuickStatsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Calls Handled",
            value = "12",
            icon = Icons.Rounded.CallMade,
            color = PrimaryIndigo
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Spam Blocked",
            value = "3",
            icon = Icons.Rounded.Block,
            color = DangerRed
        )
    }
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Box(
        modifier = modifier
            .bounceClick { }
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFD0D7FF))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF0F0F5), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = value,
                    color = Color(0xFF1E1E2C),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = Color(0xFFA0A0B0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Screening Sandbox ────────────────────────────────────────────────────────
@Composable
fun ScreeningSandbox(onScreenNumber: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Test Sandbox",
            color = Color(0xFF1E1E2C),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter a phone number...", color = Color(0xFFA0A0B0)) },
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White,
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                textColor = Color(0xFF1E1E2C)
            ),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { if (text.isNotBlank()) onScreenNumber(text) }) {
                    Icon(Icons.Rounded.Search, contentDescription = "Simulate", tint = PrimaryIndigo)
                }
            }
        )
    }
}

// ─── Quick Action Card ────────────────────────────────────────────────────────
@Composable
fun QuickActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .bounceClick(onClick = onClick)
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFD0D7FF))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF0F0F5), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = Color(0xFF1E1E2C),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFFA0A0B0),
                fontSize = 12.sp
            )
        }
    }
}
