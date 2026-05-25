package com.example.myapplication.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threat_history")
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val callerName: String?,
    val timestamp: Long,
    val verdict: String, // SAFE, SUSPICIOUS, FRAUD
    val reason: String?,
    val transcript: String?
)
