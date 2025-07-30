package com.ingredientguard.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ingredientguard.data.TextAnalyzer
import com.ingredientguard.data.models.AnalysisState
import com.ingredientguard.data.repository.HistoryRepository
import kotlinx.coroutines.launch

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val textAnalyzer = TextAnalyzer()
    private val historyRepository = HistoryRepository(application)
    
    var analysisState by mutableStateOf<AnalysisState>(AnalysisState.Idle)
        private set
    
    fun analyzeImage(uri: Uri, context: Context) {
        analysisState = AnalysisState.Loading
        
        textAnalyzer.analyzeImage(
            uri = uri,
            context = context,
            onResult = { result ->
                analysisState = AnalysisState.Success(result)
                // Salvează în istoric
                viewModelScope.launch {
                    try {
                        historyRepository.saveAnalysisResult(result, uri.toString())
                    } catch (e: Exception) {
                        // Log error but don't break the flow
                        e.printStackTrace()
                    }
                }
            },
            onError = { error ->
                analysisState = AnalysisState.Error(error.message ?: "Eroare necunoscută")
            }
        )
    }
    
    fun resetState() {
        analysisState = AnalysisState.Idle
    }
}
