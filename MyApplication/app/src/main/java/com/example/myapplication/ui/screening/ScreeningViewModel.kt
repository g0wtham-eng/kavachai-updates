package com.example.myapplication.ui.screening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.myapplication.di.NetworkModule
import android.util.Log

class ScreeningViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ScreeningState())
    val uiState = _uiState.asStateFlow()

    fun startScreening(phoneNumber: String) {
        _uiState.update { it.copy(phoneNumber = phoneNumber) }
        simulateAIAssistant(phoneNumber)
    }

    private fun simulateAIAssistant(phoneNumber: String) {
        viewModelScope.launch {
            try {
                // Call the real backend API service
                val response = NetworkModule.fraudApiService.checkCaller(phoneNumber)
                val messages = response.transcript ?: listOf(
                    "KavachAI: Hello, identifying caller...",
                    "KavachAI: Analyzing intent and voice signature..."
                )

                for (i in messages.indices) {
                    delay(2000)
                    val msg = messages[i]
                    val progressRatio = (i + 1).toFloat() / messages.size
                    
                    // Progressive reveal of status checks during the AI scan!
                    val voiceVal = if (i >= 1) (response.voiceStatus ?: "Analyzing...") else "Analyzing..."
                    val originVal = if (i >= 3) (response.originStatus ?: "Analyzing...") else "Analyzing..."
                    val dbVal = if (i >= 4) (response.dbStatus ?: "Analyzing...") else "Analyzing..."
                    
                    _uiState.update {
                        it.copy(
                            transcript = it.transcript + msg,
                            progress = progressRatio,
                            voiceStatus = voiceVal,
                            originStatus = originVal,
                            dbStatus = dbVal
                        )
                    }
                }

                val mappedVerdict = when (response.verdict.uppercase()) {
                    "SAFE" -> Verdict.SAFE
                    "SUSPICIOUS" -> Verdict.SUSPICIOUS
                    "FRAUD" -> Verdict.FRAUD
                    else -> Verdict.SUSPICIOUS
                }

                delay(1000)
                _uiState.update {
                    it.copy(
                        verdict = mappedVerdict,
                        confidenceScore = response.score,
                        voiceStatus = response.voiceStatus ?: "Real Human",
                        originStatus = response.originStatus ?: "Jio Network",
                        dbStatus = response.dbStatus ?: "Clean",
                        progress = 1.0f,
                        isAnalysisComplete = true
                    )
                }
            } catch (e: Exception) {
                Log.e("ScreeningViewModel", "Failed to query live fraud server, falling back to local simulation", e)
                
                // Fallback local simulation logic
                val scenario = when {
                    phoneNumber.endsWith("1") -> safeScenario()
                    phoneNumber.endsWith("2") -> suspiciousScenario()
                    else -> fraudScenario()
                }
                
                val fallbackVoice = when (scenario.finalVerdict) {
                    Verdict.SAFE -> "Real Human"
                    Verdict.SUSPICIOUS -> "Real Human"
                    else -> "AI Cloned (94%)"
                }
                val fallbackOrigin = when (scenario.finalVerdict) {
                    Verdict.SAFE -> "Jio Network"
                    Verdict.SUSPICIOUS -> "VoIP Network"
                    else -> "Unknown (VoIP)"
                }
                val fallbackDb = when (scenario.finalVerdict) {
                    Verdict.SAFE -> "Clean"
                    Verdict.SUSPICIOUS -> "Clean"
                    else -> "Fraud Registry"
                }

                for (i in scenario.messages.indices) {
                    delay(2000)
                    val msg = scenario.messages[i]
                    val progressRatio = (i + 1).toFloat() / scenario.messages.size
                    
                    val voiceVal = if (i >= 1) fallbackVoice else "Analyzing..."
                    val originVal = if (i >= 2) fallbackOrigin else "Analyzing..."
                    val dbVal = if (i >= 3) fallbackDb else "Analyzing..."
                    
                    _uiState.update {
                        it.copy(
                            transcript = it.transcript + msg,
                            progress = progressRatio,
                            voiceStatus = voiceVal,
                            originStatus = originVal,
                            dbStatus = dbVal
                        )
                    }
                }

                delay(1000)
                _uiState.update {
                    it.copy(
                        verdict = scenario.finalVerdict,
                        confidenceScore = scenario.score,
                        voiceStatus = fallbackVoice,
                        originStatus = fallbackOrigin,
                        dbStatus = fallbackDb,
                        progress = 1.0f,
                        isAnalysisComplete = true
                    )
                }
            }
        }
    }

    private fun safeScenario() = Scenario(
        messages = listOf(
            "KavachAI: Hello, screening call for verification...",
            "Caller: Hey! It's me, just calling to ask if we are meeting for dinner tonight.",
            "KavachAI: Voice signature matched. Contact is whitelisted.",
            "KavachAI: VERDICT: Safe personal contact."
        ),
        finalVerdict = Verdict.SAFE,
        score = 0.01f
    )

    private fun suspiciousScenario() = Scenario(
        messages = listOf(
            "KavachAI: Hello, this is KavachAI Screening. State your purpose.",
            "Caller: I'm calling about a lucky draw promotion you won!",
            "KavachAI: Analyzing intent and checking databases...",
            "KavachAI: Suspicious request. Potential marketing call or low-risk scam."
        ),
        finalVerdict = Verdict.SUSPICIOUS,
        score = 0.62f
    )

    private fun fraudScenario() = Scenario(
        messages = listOf(
            "KavachAI: Hello, this is KavachAI screening assistant. State your name and purpose.",
            "Caller (Robo-AI): This is an automated alert from Federal Bank Security. We detected a suspicious transfer of Rs. 45,000.",
            "KavachAI: Checking server database... No active bank request found. Analyzing voice clone signature...",
            "Caller (Robo-AI): System alert. Repeat the OTP immediately to block this transfer and prevent account suspension.",
            "KavachAI: WARNING: Deepfake voice synthesis probability: 96%. Phishing signature verified (OTP demand).",
            "KavachAI: TERMINATING CALL. Blocked banking threat."
        ),
        finalVerdict = Verdict.FRAUD,
        score = 0.98f
    )

    private data class Scenario(
        val messages: List<String>,
        val finalVerdict: Verdict,
        val score: Float
    )
}
