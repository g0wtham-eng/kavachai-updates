package com.example.myapplication.ui.screening

enum class Verdict {
    SAFE, SUSPICIOUS, FRAUD, ANALYZING
}

data class ScreeningState(
    val phoneNumber: String = "",
    val verdict: Verdict = Verdict.ANALYZING,
    val transcript: List<String> = emptyList(),
    val isAnalysisComplete: Boolean = false,
    val confidenceScore: Float = 0f,
    val voiceStatus: String = "Analyzing...",
    val originStatus: String = "Analyzing...",
    val dbStatus: String = "Analyzing...",
    val progress: Float = 0f
)
