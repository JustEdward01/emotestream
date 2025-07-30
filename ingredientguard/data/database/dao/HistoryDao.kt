package com.ingredientguard.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.ingredientguard.data.repository.ScanHistoryItem
import java.util.Date

@Dao
interface HistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanHistoryItem>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    suspend fun getAllScansOnce(): List<ScanHistoryItem>

    @Query("SELECT * FROM scan_history WHERE fullText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchScans(query: String): List<ScanHistoryItem>

    @Query("SELECT * FROM scan_history WHERE hasAllergens = 1 ORDER BY timestamp DESC")
    suspend fun getScansWithAllergens(): List<ScanHistoryItem>

    @Query("SELECT * FROM scan_history WHERE hasPersonalAllergens = 1 ORDER BY timestamp DESC")
    suspend fun getScansWithPersonalAllergens(): List<ScanHistoryItem>

    @Insert
    suspend fun insertScan(scan: ScanHistoryItem): Long

    @Delete
    suspend fun deleteScan(scan: ScanHistoryItem)

    @Query("DELETE FROM scan_history WHERE timestamp < :date")
    suspend fun deleteOldScans(date: Date)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScans()
}