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
    fun processCallerVoice(context: android.content.Context, text: String) {
        addTranscriptMessage("Caller: $text")
        saveMessageToFile(context, "Caller: $text")

        // Simple mocked LLM intent detection for the caller
        viewModelScope.launch {
            val lower = text.lowercase()
            val reply = when {
                lower.contains("amazon") || lower.contains("flipkart") || lower.contains("delivery") -> {
                    "Delivery intent detected. Could you please confirm the order number or what you are delivering?"
                }
                lower.contains("bank") || lower.contains("otp") || lower.contains("card") -> {
                    "Financial inquiry detected. Mr. Gowtham does not share account details over phone. What is your exact branch name?"
                }
                lower.contains("interview") || lower.contains("hr") || lower.contains("job") -> {
                    "Recruitment inquiry detected. Can you state your company name and the role you are hiring for?"
                }
                else -> null // AI doesn't auto-reply to everything; it waits for the user or specific keywords
            }

            if (reply != null) {
                delay(1500)
                addTranscriptMessage("KavachAI: $reply")
                saveMessageToFile(context, "KavachAI: $reply")
                val intentAgent = android.content.Intent("com.canara.kavachai.NEW_TRANSCRIPT_TTS").apply {
                    putExtra("message", reply)
                    putExtra("isAI", true)
                }
                context.sendBroadcast(intentAgent)
            }
        }
    }

    fun sendUserMessage(context: android.content.Context, message: String) {
        // 1. Add user message to transcript (Silent instruction to AI)
        addTranscriptMessage("You (Instruction): $message")
        saveMessageToFile(context, "Owner: $message")
        
        // 2. Respond to the user input by generating a polished out-loud phrase
        viewModelScope.launch {
            val lower = message.lowercase()
            val reply = when {
                lower.contains("busy") || lower.contains("later") -> {
                    "I'm sorry, Mr. Gowtham is currently busy and unable to take this call. I will notify him to return your call when he is available. Thank you."
                }
                lower.contains("ask purpose") || lower.contains("why") || lower.contains("purpose") -> {
                    "Could you please clarify the exact purpose of your call? I need this information to notify Mr. Gowtham."
                }
                lower.contains("not interested") || lower.contains("no thanks") -> {
                    "Mr. Gowtham is not interested in this offer at the moment. Please remove his number from your calling list. Thank you and goodbye."
                }
                lower.contains("block") || lower.contains("disconnect") || lower.contains("hang") -> {
                    "SECURITY ALERT: This call has been marked as spam. Terminating connection now."
                }
                else -> {
                    // Fallback to echo a polished version of what the user typed
                    "Mr. Gowtham's instruction: $message"
                }
            }
            
            // Add the generated response to the chat
            addTranscriptMessage("KavachAI: $reply")
            saveMessageToFile(context, "KavachAI: $reply")

            // Broadcast agent response to speak aloud
            val intentAgent = android.content.Intent("com.canara.kavachai.NEW_TRANSCRIPT_TTS").apply {
                putExtra("message", reply)
                putExtra("isAI", true)
            }
            context.sendBroadcast(intentAgent)
            
            // If they said block/disconnect, trigger disconnect via broadcast
            if (lower.contains("block") || lower.contains("disconnect") || lower.contains("hang") || lower.contains("not interested")) {
                delay(5000) // wait for TTS to finish before hanging up
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
