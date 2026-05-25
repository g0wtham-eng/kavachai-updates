package com.example.myapplication.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entities.ThreatEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val threatDao = AppDatabase.getDatabase(application).threatDao()

    val history: StateFlow<List<ThreatEntity>> = threatDao.getAllThreats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteHistory() {
        viewModelScope.launch {
            threatDao.clearHistory()
        }
    }

    fun addMockThreat() {
        viewModelScope.launch {
            threatDao.insertThreat(
                ThreatEntity(
                    phoneNumber = "+91 98765 43210",
                    callerName = "Mock Scammer",
                    timestamp = System.currentTimeMillis(),
                    verdict = "FRAUD",
                    reason = "Synthetic voice detected",
                    transcript = "AI detected a fraudulent OTP request."
                )
            )
        }
    }
}
