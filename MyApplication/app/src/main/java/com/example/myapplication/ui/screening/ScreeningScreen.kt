package com.example.myapplication.ui.screening

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun ScreeningScreen(
    state: ScreeningState,
    onClose: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ScreeningActions(state, onClose)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Text(
                text = "KavachAI Active Screening",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = state.phoneNumber,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Analysis Progress Bar
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Analyzing call integrity...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            // Voice, Origin, and Database checks
            StatusCheckRows(state)

            // AI Transcript Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                TranscriptList(state.transcript)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Verdict Card
            VerdictCard(state)
        }
    }
}

@Composable
fun TranscriptList(transcript: List<String>) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(transcript.size) {
        if (transcript.isNotEmpty()) {
            listState.animateScrollToItem(transcript.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(transcript) { message ->
            val isAI = message.startsWith("KavachAI:")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (isAI) 32.dp else 0.dp, start = if (isAI) 0.dp else 32.dp),
                contentAlignment = if (isAI) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isAI) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAI) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun VerdictCard(state: ScreeningState) {
    val (color, icon, label, description) = when (state.verdict) {
        Verdict.ANALYZING -> Quadruple(
            MaterialTheme.colorScheme.primary,
            Icons.Rounded.Search,
            "Analyzing Caller...",
            "AI assistant is talking to the caller to verify intent."
        )
        Verdict.SAFE -> Quadruple(
            Color(0xFF4CAF50),
            Icons.Rounded.CheckCircle,
            "Verdict: Safe",
            "This caller appears legitimate. You can answer safely."
        )
        Verdict.SUSPICIOUS -> Quadruple(
            Color(0xFFFF9800),
            Icons.Rounded.Warning,
            "Verdict: Suspicious",
            "Potential telemarketing or low-risk scam detected."
        )
        Verdict.FRAUD -> Quadruple(
            Color(0xFFF44336),
            Icons.Rounded.Dangerous,
            "Verdict: FRAUD DETECTED",
            "High probability of deepfake voice and OTP scam."
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(24.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(color))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScreeningActions(state: ScreeningState, onClose: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Reject Button
            LargeActionCircle(
                icon = Icons.Rounded.CallEnd,
                color = Color(0xFFF44336),
                label = "Reject",
                onClick = onClose
            )

            // Answer Button (only if not confirmed fraud)
            if (state.verdict != Verdict.FRAUD) {
                LargeActionCircle(
                    icon = Icons.Rounded.Call,
                    color = Color(0xFF4CAF50),
                    label = "Answer",
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
fun LargeActionCircle(
    icon: ImageVector,
    color: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = color)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun PreviewScreeningFraud() {
    MyApplicationTheme {
        ScreeningScreen(
            state = ScreeningState(
                phoneNumber = "+91 98765 43210",
                verdict = Verdict.FRAUD,
                transcript = listOf(
                    "KavachAI: Hello, identifying caller...",
                    "Caller: Your account is blocked. Tell me the OTP I just sent.",
                    "KavachAI: ALERT: OTP request detected. Analyzing voice...",
                    "KavachAI: Synthetic voice detected. Deepfake probability: High."
                ),
                isAnalysisComplete = true,
                confidenceScore = 0.98f
            ),
            onClose = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun PreviewScreeningAnalyzing() {
    MyApplicationTheme {
        ScreeningScreen(
            state = ScreeningState(
                phoneNumber = "+91 12345 67890",
                verdict = Verdict.ANALYZING,
                transcript = listOf(
                    "KavachAI: Hello, whom am I speaking with?",
                    "Caller: Hi, this is Rajesh from your local community center."
                )
            ),
            onClose = {}
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun StatusCheckRows(state: ScreeningState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusRow(label = "Voice Signature", status = state.voiceStatus)
        StatusRow(label = "Origin Network", status = state.originStatus)
        StatusRow(label = "Security Registry", status = state.dbStatus)
    }
}

@Composable
fun StatusRow(label: String, status: String) {
    val (color, icon) = when {
        status == "Analyzing..." -> Pair(MaterialTheme.colorScheme.primary, Icons.Rounded.Sync)
        status.contains("Clean") || status.contains("Real") || status.contains("Jio") || status.contains("Airtel") || status.contains("Family") || status.contains("Helpline") -> 
            Pair(Color(0xFF4CAF50), Icons.Rounded.CheckCircle)
        status.contains("Uncertain") || status.contains("VoIP") || status.contains("Suspicious") ->
            Pair(Color(0xFFFF9800), Icons.Rounded.Warning)
        else -> Pair(Color(0xFFF44336), Icons.Rounded.Dangerous)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = status, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
