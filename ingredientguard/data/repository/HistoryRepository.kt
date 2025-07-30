package com.ingredientguard.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.ingredientguard.data.database.AppDatabase
import com.ingredientguard.data.analyzer.EnhancedAnalysisResult
import com.ingredientguard.data.models.*
import java.util.Date

class HistoryRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val historyDao = database.historyDao()

    // Metode pentru sistemul nou
    fun getAllScans(): Flow<List<ScanHistoryItem>> = historyDao.getAllScans()

    suspend fun getAllScansOnce(): List<ScanHistoryItem> = historyDao.getAllScansOnce()

    suspend fun searchScans(query: String): List<ScanHistoryItem> = historyDao.searchScans(query)

    suspend fun getScansWithAllergens(): List<ScanHistoryItem> = historyDao.getScansWithAllergens()

    suspend fun getScansWithPersonalAllergens(): List<ScanHistoryItem> = historyDao.getScansWithPersonalAllergens()

    suspend fun saveScanResult(item: ScanHistoryItem): Long = historyDao.insertScan(item)

    suspend fun deleteScan(item: ScanHistoryItem) = historyDao.deleteScan(item)

    // Compatibilitate cu sistemul vechi
    suspend fun saveAnalysisResult(result: AnalysisResult, imagePath: String? = null): Long {
        val historyItem = ScanHistoryItem(
            timestamp = Date(),
            fullText = result.fullText,
            imagePath = imagePath,
            ingredients = result.ingredients,
            allergens = result.allergens,
            hasAllergens = result.allergens.isNotEmpty(),
            hasPersonalAllergens = false,
            personalAllergensFound = emptyList()
        )
        return historyDao.insertScan(historyItem)
    }

    // Enhanced method pentru rezultatele noi
    suspend fun saveEnhancedAnalysisResult(
        result: EnhancedAnalysisResult,
        imageUri: String
    ): Long {
        val historyItem = ScanHistoryItem(
            timestamp = Date(),
            fullText = result.fullText,
            imagePath = imageUri,
            ingredients = result.ingredients,
            allergens = result.allergens,
            hasAllergens = result.allergens.isNotEmpty(),
            hasPersonalAllergens = result.personalAllergensDetected,
            personalAllergensFound = result.personalAllergensList
        )

        return historyDao.insertScan(historyItem)
    }
}