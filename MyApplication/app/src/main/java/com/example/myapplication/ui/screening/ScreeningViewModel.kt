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

    fun setPhoneNumber(phoneNumber: String) {
        _uiState.update { it.copy(phoneNumber = phoneNumber) }
    }

    fun addTranscriptMessage(message: String) {
        val currentSize = _uiState.value.transcript.size
        // Fake progressive updates based on number of messages for visual effect
        val progressRatio = minOf((currentSize + 1).toFloat() / 5f, 1.0f)
        
        val voiceVal = if (currentSize >= 1) "Analyzing Voice..." else "Connecting..."
        val originVal = if (currentSize >= 2) "Jio / Airtel Check..." else "Waiting..."
        val dbVal = if (currentSize >= 3) "Fraud DB Check..." else "Waiting..."

        _uiState.update {
            it.copy(
                transcript = it.transcript + message,
                progress = progressRatio,
                voiceStatus = voiceVal,
                originStatus = originVal,
                dbStatus = dbVal
            )
        }

        if (message.contains("TERMINATING CALL") || message.contains("VERDICT")) {
            val isFraud = message.contains("TERMINATING CALL")
            _uiState.update {
                it.copy(
                    verdict = if (isFraud) Verdict.FRAUD else Verdict.SAFE,
                    confidenceScore = if (isFraud) 0.98f else 0.02f,
                    voiceStatus = if (isFraud) "AI Cloned (94%)" else "Real Human",
                    originStatus = if (isFraud) "Unknown (VoIP)" else "Jio Network",
                    dbStatus = if (isFraud) "Fraud Registry" else "Clean",
                    progress = 1.0f,
                    isAnalysisComplete = true
                )
            }
        }
    }
    fun sendUserMessage(context: android.content.Context, message: String) {
        // 1. Add user message to transcript and file
        addTranscriptMessage("Caller: $message")
        saveMessageToFile(context, "Caller: $message")

        // Broadcast user message to speak aloud (using lower pitch/scammer settings)
        val intentUser = android.content.Intent("com.canara.kavachai.NEW_TRANSCRIPT_TTS").apply {
            putExtra("message", message)
            putExtra("isAI", false)
        }
        context.sendBroadcast(intentUser)
        
        // 2. Respond to the user input
        viewModelScope.launch {
            delay(1000)
            val lower = message.lowercase()
            val reply = when {
                lower.contains("who") || lower.contains("name") || lower.contains("identity") -> {
                    "Scanning Caller ID... Traced connection to suspect local VoIP network."
                }
                lower.contains("safe") || lower.contains("legit") || lower.contains("trust") -> {
                    "Threat confidence score is low. Advise caution."
                }
                lower.contains("block") || lower.contains("disconnect") || lower.contains("hang") -> {
                    "ALERT: Phishing threat level critical. Terminating call now."
                }
                lower.contains("hello") || lower.contains("hi") -> {
                    "KavachAI active. Interrogating incoming line. State security check request."
                }
                else -> {
                    "Analyzing caller voice profile... No threat match found in local registry."
                }
            }
            addTranscriptMessage("KavachAI: $reply")
            saveMessageToFile(context, "KavachAI: $reply")

            // Broadcast agent response to speak aloud (using high pitch/AI settings)
            val intentAgent = android.content.Intent("com.canara.kavachai.NEW_TRANSCRIPT_TTS").apply {
                putExtra("message", reply)
                putExtra("isAI", true)
            }
            context.sendBroadcast(intentAgent)
            
            // If they said block/disconnect, trigger disconnect via broadcast
            if (lower.contains("block") || lower.contains("disconnect") || lower.contains("hang")) {
                delay(1000)
                val disconnectIntent = android.content.Intent("com.canara.kavachai.TRIGGER_DISCONNECT")
                context.sendBroadcast(disconnectIntent)
            }
        }
    }

    private fun saveMessageToFile(context: android.content.Context, line: String) {
        try {
            val file = java.io.File(context.filesDir, "conversation_log.txt")
            file.appendText("$line\n")
            Log.i("ScreeningViewModel", "Saved line: $line to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("ScreeningViewModel", "Failed to save to log file", e)
        }
    }
}
