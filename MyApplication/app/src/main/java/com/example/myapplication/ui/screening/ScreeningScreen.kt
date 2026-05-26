package com.example.myapplication.ui.screening

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@Composable
fun ScreeningScreen(
    state: ScreeningState,
    onClose: () -> Unit
) {
    val verdictColor = when (state.verdict) {
        Verdict.ANALYZING  -> CanaraBlue
        Verdict.SAFE       -> NeonGreen
        Verdict.SUSPICIOUS -> NeonAmber
        Verdict.FRAUD      -> NeonRed
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring1Scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseOutCirc), RepeatMode.Restart),
        label = "ring1"
    )
    val ring2Scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.65f,
        animationSpec = infiniteRepeatable(tween(1800, 300, easing = EaseOutCirc), RepeatMode.Restart),
        label = "ring2"
    )
    val ring1Alpha by pulse.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseOutCirc), RepeatMode.Restart),
        label = "alpha1"
    )
    val ring2Alpha by pulse.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, 300, easing = EaseOutCirc), RepeatMode.Restart),
        label = "alpha2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBg)
    ) {
        // ── Glow blob behind caller ring ──
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopCenter)
                .offset(y = 60.dp)
                .blur(100.dp)
                .background(verdictColor.copy(alpha = 0.2f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // ── AI Label ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Shield, null, tint = CanaraBlue, modifier = Modifier.size(16.dp))
                Text(
                    "CANARA AI SCREENING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CanaraBlue,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Animated Caller Ring ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Pulse rings
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(ring1Scale)
                        .background(verdictColor.copy(alpha = ring1Alpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(ring2Scale)
                        .background(verdictColor.copy(alpha = ring2Alpha), CircleShape)
                )
                // Main circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    verdictColor.copy(alpha = 0.3f),
                                    verdictColor.copy(alpha = 0.05f)
                                )
                            ),
                            CircleShape
                        )
                        .border(2.dp, verdictColor.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = verdictColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Caller Number ──
            Text(
                text = state.phoneNumber,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Unknown Caller",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Progress Bar ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = verdictColor,
                    trackColor = SurfaceLight
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Analyzing...", fontSize = 11.sp, color = TextSecondary)
                    Text("${(state.progress * 100).toInt()}%", fontSize = 11.sp, color = verdictColor, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Status Chips ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(modifier = Modifier.weight(1f), label = state.voiceStatus, icon = Icons.Rounded.RecordVoiceOver)
                StatusChip(modifier = Modifier.weight(1f), label = state.originStatus, icon = Icons.Rounded.Wifi)
                StatusChip(modifier = Modifier.weight(1f), label = state.dbStatus, icon = Icons.Rounded.Dataset)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Chat Transcript ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .border(1.dp, verdictColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(12.dp)
            ) {
                TranscriptChatList(transcript = state.transcript, accentColor = verdictColor)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Verdict Banner ──
            VerdictBanner(state = state, accentColor = verdictColor)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Action Buttons ──
            ActionButtonsRow(state = state, onClose = onClose, accentColor = verdictColor)

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ─── Chat List ────────────────────────────────────────────────────────────────
@Composable
fun TranscriptChatList(transcript: List<String>, accentColor: Color) {
    val listState = rememberLazyListState()
    LaunchedEffect(transcript.size) {
        if (transcript.isNotEmpty()) listState.animateScrollToItem(transcript.size - 1)
    }

    if (transcript.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Mic, null, tint = TextDim, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("AI is listening...", color = TextDim, fontSize = 13.sp)
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(transcript) { message ->
            val isAI = message.startsWith("KavachAI:")
            val isWarning = message.contains("WARNING") || message.contains("CRITICAL") || message.contains("TERMINATING")
            val cleanText = message
                .removePrefix("KavachAI:")
                .removePrefix("Caller:")
                .removePrefix("Caller (Robo-AI):")
                .trim()

            val bubbleColor = when {
                isAI && isWarning -> NeonRed.copy(alpha = 0.12f)
                isAI              -> accentColor.copy(alpha = 0.1f)
                else              -> SurfaceMid
            }
            val textColor = when {
                isAI && isWarning -> NeonRed
                isAI              -> TextPrimary
                else              -> TextSecondary
            }
            val borderColor = when {
                isAI && isWarning -> NeonRed.copy(0.4f)
                isAI              -> accentColor.copy(0.25f)
                else              -> TextDim.copy(0.2f)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
            ) {
                if (isAI) {
                    // AI avatar
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(accentColor.copy(0.15f), CircleShape)
                            .border(1.dp, accentColor.copy(0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.SmartToy, null, tint = accentColor, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = if (isAI) Alignment.Start else Alignment.End
                ) {
                    Text(
                        text = if (isAI) "Canara AI" else "Caller",
                        fontSize = 10.sp,
                        color = TextDim,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp, topEnd = 16.dp,
                                    bottomStart = if (isAI) 4.dp else 16.dp,
                                    bottomEnd = if (isAI) 16.dp else 4.dp
                                )
                            )
                            .background(bubbleColor)
                            .border(
                                1.dp,
                                borderColor,
                                RoundedCornerShape(
                                    topStart = 16.dp, topEnd = 16.dp,
                                    bottomStart = if (isAI) 4.dp else 16.dp,
                                    bottomEnd = if (isAI) 16.dp else 4.dp
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = cleanText,
                            fontSize = 13.sp,
                            color = textColor,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Status Chip ─────────────────────────────────────────────────────────────
@Composable
fun StatusChip(modifier: Modifier, label: String, icon: ImageVector) {
    val color = when {
        label == "Analyzing..."   -> CanaraBlue
        label.contains("Fraud") || label.contains("AI Cloned") || label.contains("VoIP") -> NeonRed
        label.contains("Uncertain") || label.contains("Suspicious") -> NeonAmber
        else -> NeonGreen
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

// ─── Verdict Banner ───────────────────────────────────────────────────────────
@Composable
fun VerdictBanner(state: ScreeningState, accentColor: Color) {
    val (icon, label, desc) = when (state.verdict) {
        Verdict.ANALYZING  -> Triple(Icons.Rounded.Search,    "Analyzing Caller...",   "AI is actively interrogating the caller")
        Verdict.SAFE       -> Triple(Icons.Rounded.CheckCircle, "Verdict: SAFE",        "Caller appears legitimate — safe to answer")
        Verdict.SUSPICIOUS -> Triple(Icons.Rounded.Warning,   "Verdict: SUSPICIOUS",   "Potential spam or unsolicited call detected")
        Verdict.FRAUD      -> Triple(Icons.Rounded.Dangerous,  "FRAUD DETECTED",        "Canara Bank phishing attempt blocked!")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = accentColor)
            Text(desc, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ─── Action Buttons Row ───────────────────────────────────────────────────────
@Composable
fun ActionButtonsRow(state: ScreeningState, onClose: () -> Unit, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // End Call
        GlowActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.CallEnd,
            label = "End",
            color = NeonRed,
            onClick = onClose
        )
        // Take Over
        GlowActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.PersonSearch,
            label = "Take Over",
            color = CanaraBlue,
            onClick = onClose
        )
        // Answer (only if not fraud)
        if (state.verdict != Verdict.FRAUD) {
            GlowActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Call,
                label = "Answer",
                color = NeonGreen,
                onClick = onClose
            )
        }
    }
}

@Composable
fun GlowActionButton(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Glow
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .blur(16.dp)
                    .background(color.copy(alpha = 0.4f), CircleShape)
            )
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = color.copy(alpha = 0.15f))
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
            )
        }
        Text(label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
    }
}
