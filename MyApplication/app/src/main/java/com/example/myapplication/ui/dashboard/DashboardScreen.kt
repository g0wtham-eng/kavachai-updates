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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            .background(BlackBg)
    ) {
        // Glowing background blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .blur(120.dp)
                .background(KavachRed.copy(alpha = 0.15f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .blur(120.dp)
                .background(KavachRedAccent.copy(alpha = 0.08f), CircleShape)
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
                        title = "Threat Log",
                        subtitle = "Past calls",
                        color = KavachRed,
                        onClick = onNavigateToHistory
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.BugReport,
                        title = "Test AI",
                        subtitle = "Simulate call",
                        color = NeonGreen,
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
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Animated shield icon
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .scale(scale)
                            .blur(16.dp)
                            .background(KavachRed.copy(alpha = 0.5f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.radialGradient(listOf(KavachRed.copy(0.3f), Color.Transparent)),
                                CircleShape
                            )
                            .border(1.dp, KavachRed.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = KavachRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "KavachAI",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Bank Security Assistant",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = KavachRed,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Live indicator
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scale)
                    .background(if (isEnabled) NeonGreen else NeonAmber, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isEnabled) "LIVE" else "SETUP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) NeonGreen else NeonAmber,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// ─── Status Card ─────────────────────────────────────────────────────────────
@Composable
fun StatusCard(isEnabled: Boolean, onClick: () -> Unit) {
    val statusColor = if (isEnabled) NeonGreen else NeonAmber
    val icon = if (isEnabled) Icons.Rounded.VerifiedUser else Icons.Rounded.GppMaybe
    val title = if (isEnabled) "System Protected" else "Action Required"
    val desc = if (isEnabled)
        "KavachAI is actively screening all incoming calls"
    else
        "Tap to enable call screening protection"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(
                1.dp,
                Brush.linearGradient(listOf(statusColor.copy(0.6f), statusColor.copy(0.1f))),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(statusColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = statusColor, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text(desc, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (!isEnabled) {
            Icon(
                Icons.Rounded.ArrowForwardIos,
                null,
                tint = TextDim,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(16.dp)
            )
        }
    }
}

// ─── Quick Stats ─────────────────────────────────────────────────────────────
@Composable
fun QuickStatsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(modifier = Modifier.weight(1f), label = "Screened", value = "128", color = KavachRed)
        StatChip(modifier = Modifier.weight(1f), label = "Blocked", value = "14", color = NeonRed)
        StatChip(modifier = Modifier.weight(1f), label = "Safe", value = "112", color = NeonGreen)
    }
}

@Composable
fun StatChip(modifier: Modifier, label: String, value: String, color: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = color)
        Text(label, fontSize = 11.sp, color = TextSecondary, letterSpacing = 0.5.sp)
    }
}

// ─── Screening Sandbox ────────────────────────────────────────────────────────
@Composable
fun ScreeningSandbox(onScreenNumber: (String) -> Unit) {
    var inputNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, KavachRed.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.PhoneInTalk, null, tint = KavachRed, modifier = Modifier.size(22.dp))
            Text("AI Call Sandbox", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        }

        Text(
            "Enter any number to simulate a call and watch the AI interrogate the caller in real-time:",
            fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp
        )

        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceMid)
                .border(1.dp, if (inputNumber.isNotEmpty()) KavachRed.copy(0.5f) else TextDim.copy(0.3f), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Dialpad, null, tint = KavachRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = inputNumber,
                onValueChange = { inputNumber = it },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium
                ),
                decorationBox = { inner ->
                    if (inputNumber.isEmpty()) {
                        Text("+91 99999 00005", color = TextDim, fontSize = 15.sp)
                    }
                    inner()
                },
                singleLine = true
            )
            if (inputNumber.isNotEmpty()) {
                IconButton(onClick = { inputNumber = "" }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Hints
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HintRow("Ends in 1", "SAFE — Verified contact", NeonGreen)
            HintRow("Ends in 2", "SUSPICIOUS — Loan offer", NeonAmber)
            HintRow("Others",    "FRAUD — phishing attempt", NeonRed)
        }

        // Screen button
        Button(
            onClick = {
                val num = if (inputNumber.isBlank()) "+91 99999 00005" else inputNumber
                onScreenNumber(num)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KavachRed)
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start KavachAI Screening", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun HintRow(tag: String, desc: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(
            text = "$tag  →  $desc",
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

// ─── Quick Action Cards ───────────────────────────────────────────────────────
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
            .height(100.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
