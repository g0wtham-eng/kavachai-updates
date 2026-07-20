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
}
