package com.ingredientguard.data.analyzer

import com.ingredientguard.data.models.DetectedIngredient
import com.ingredientguard.data.models.DetectedAllergen

data class EnhancedAnalysisResult(
    val fullText: String,
    val ingredients: List<DetectedIngredient>,
    val allergens: List<DetectedAllergen>,
    val personalAllergensDetected: Boolean = false,
    val personalAllergensList: List<String> = emptyList()
)