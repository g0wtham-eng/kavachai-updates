package com.example.myapplication

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.rememberNavBackStack
import com.example.myapplication.ui.navigation.Destination
import com.example.myapplication.ui.navigation.KavachNavGraph
import com.example.myapplication.ui.theme.BlackBg
import com.example.myapplication.ui.theme.KavachRed
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.NeonGreen
import com.example.myapplication.ui.theme.SurfaceDark
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                var isSetupRequired by remember { mutableStateOf(true) }
                
                LaunchedEffect(Unit) {
                    try {
                        com.example.myapplication.util.UpdateHelper.checkForUpdates(context)
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error checking for updates", e)
                    }
                }

                if (isSetupRequired) {
                    KavachSetupScreen(onSetupComplete = { isSetupRequired = false })
                } else {
                    val backStack = rememberNavBackStack(Destination.Dashboard)
                    KavachNavGraph(backStack = backStack)
                }
            }
        }
    }
}

@Composable
fun KavachSetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State trackers for the 3 main permission groups
    var hasCorePermissions by remember { mutableStateOf(checkCorePermissions(context)) }
    var hasDialerRole by remember { mutableStateOf(checkDialerRole(context)) }
    var hasOverlayPermission by remember { mutableStateOf(checkOverlayPermission(context)) }

    // Re-check permissions when activity resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCorePermissions = checkCorePermissions(context)
                hasDialerRole = checkDialerRole(context)
                hasOverlayPermission = checkOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allDone = hasCorePermissions && hasDialerRole && hasOverlayPermission

    // Launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasCorePermissions = checkCorePermissions(context) }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasDialerRole = checkDialerRole(context) }

    // Floating animation for logo
    val infiniteTransition = rememberInfiniteTransition()
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Scaffold(containerColor = BlackBg) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Animated Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(logoScale)
                    .background(KavachRed.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, KavachRed.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = KavachRed
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Welcome to KavachAI",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            
            Text(
                text = "To unleash the full power of your AI receptionist, please grant the following permissions.",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Permission Checklist
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                
                PermissionCheckRow(
                    title = "Core Permissions",
                    description = "Phone, Contacts, Call Log",
                    isGranted = hasCorePermissions,
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.READ_CALL_LOG,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.ANSWER_PHONE_CALLS,
                                Manifest.permission.RECORD_AUDIO
                            )
                        )
                    }
                )

                PermissionCheckRow(
                    title = "Default Phone App",
                    description = "Required to intercept and screen calls",
                    isGranted = hasDialerRole,
                    onClick = {
                        val roleManager = context.getSystemService(RoleManager::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
                            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                            roleLauncher.launch(intent)
                        }
                    }
                )

                PermissionCheckRow(
                    title = "Display Over Other Apps",
                    description = "Required for the live chat bubble",
                    isGranted = hasOverlayPermission,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Continue Button (Only shown when all permissions granted)
            AnimatedVisibility(
                visible = allDone,
                enter = fadeIn(tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = spring())
            ) {
                Button(
                    onClick = onSetupComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KavachRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Enter Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            
            // Temporary skip button for debugging
            if (!allDone) {
                TextButton(onClick = onSetupComplete) {
                    Text("Skip for now (Debug)", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun PermissionCheckRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isGranted) NeonGreen.copy(alpha = 0.1f) else SurfaceDark
    val borderColor = if (isGranted) NeonGreen.copy(alpha = 0.3f) else SurfaceDark.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !isGranted, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (isGranted) NeonGreen else TextPrimary, fontSize = 16.sp)
            Text(description, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(if (isGranted) NeonGreen else Color.Transparent, CircleShape)
                .border(if (isGranted) 0.dp else 1.dp, TextSecondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isGranted,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Granted", tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun checkCorePermissions(context: Context): Boolean {
    val reqs = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.RECORD_AUDIO
    )
    return reqs.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
}

private fun checkDialerRole(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        return roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
    }
    return true
}

private fun checkOverlayPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return Settings.canDrawOverlays(context)
    }
    return true
}
