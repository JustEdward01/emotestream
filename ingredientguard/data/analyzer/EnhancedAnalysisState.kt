package com.ingredientguard.data.analyzer

sealed class EnhancedAnalysisState {
    data object Idle : EnhancedAnalysisState()
    data object Loading : EnhancedAnalysisState()
    data class Success(val result: EnhancedAnalysisResult) : EnhancedAnalysisState()
    data class Error(val message: String) : EnhancedAnalysisState()
}