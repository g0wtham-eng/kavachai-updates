package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entities.ThreatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatDao {
    @Query("SELECT * FROM threat_history ORDER BY timestamp DESC")
    fun getAllThreats(): Flow<List<ThreatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreat(threat: ThreatEntity)

    @Query("DELETE FROM threat_history")
    suspend fun clearHistory()
}
