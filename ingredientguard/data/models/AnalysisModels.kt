package com.ingredientguard.data.models

sealed class AnalysisState {
    data object Idle : AnalysisState()
    data object Loading : AnalysisState()
    data class Success(val result: AnalysisResult) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

data class AnalysisResult(
    val fullText: String,
    val ingredients: List<DetectedIngredient>,
    val allergens: List<DetectedAllergen>
)

data class DetectedIngredient(
    val text: String,
    val confidence: Float = 0.8f,
    val isAllergen: Boolean = false
)

data class DetectedAllergen(
    val name: String,
    val severity: AllergenSeverity = AllergenSeverity.MEDIUM
)

enum class AllergenSeverity { LOW, MEDIUM, HIGH }
