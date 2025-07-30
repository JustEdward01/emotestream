package com.ingredientguard.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ingredientguard.data.analyzer.EnhancedTextAnalyzer
import com.ingredientguard.data.analyzer.EnhancedAnalysisResult
import com.ingredientguard.data.analyzer.EnhancedAnalysisState
import com.ingredientguard.data.repository.HistoryRepository
import com.ingredientguard.data.repository.UserRepository
import com.ingredientguard.utils.NotificationHelper
import kotlinx.coroutines.launch

class EnhancedScannerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val textAnalyzer = EnhancedTextAnalyzer(application)
    private val historyRepository = HistoryRepository(application)
    private val userRepository = UserRepository(application)
    private val notificationHelper = NotificationHelper(application)
    
    var analysisState by mutableStateOf<EnhancedAnalysisState>(EnhancedAnalysisState.Idle)
        private set
    
    fun analyzeImage(uri: Uri) {
        analysisState = EnhancedAnalysisState.Loading
        
        textAnalyzer.analyzeImage(
            uri = uri,
            onResult = { result ->
                analysisState = EnhancedAnalysisState.Success(result)
                
                // Handle personal allergen notifications
                if (result.personalAllergensDetected) {
                    notificationHelper.showAllergenAlert(
                        personalAllergens = result.personalAllergensList,
                        productName = "produsul scanat"
                    )
                }
                
                // Save to history with enhanced information
                viewModelScope.launch {
                    try {
                        historyRepository.saveEnhancedAnalysisResult(result, uri.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onError = { error ->
                analysisState = EnhancedAnalysisState.Error(error.message ?: "Eroare necunoscută")
            }
        )
    }
    
    fun resetState() {
        analysisState = EnhancedAnalysisState.Idle
    }
    
    // Convert to legacy AnalysisResult for backward compatibility
    fun toLegacyAnalysisResult(enhanced: EnhancedAnalysisResult): com.ingredientguard.data.models.AnalysisResult {
        return com.ingredientguard.data.models.AnalysisResult(
            fullText = enhanced.fullText,
            ingredients = enhanced.ingredients,
            allergens = enhanced.allergens
        )
    }
}