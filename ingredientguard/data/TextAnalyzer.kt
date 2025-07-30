package com.ingredientguard.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ingredientguard.data.models.*

class TextAnalyzer {
    
    fun analyzeImage(
        uri: Uri,
        context: Context,
        onResult: (AnalysisResult) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    val result = processText(fullText)
                    onResult(result)
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }
    
    private fun processText(text: String): AnalysisResult {
        val mockIngredients = extractMockIngredients(text)
        val mockAllergens = extractMockAllergens(text)
        
        return AnalysisResult(
            fullText = text,
            ingredients = mockIngredients,
            allergens = mockAllergens
        )
    }
    
    private fun extractMockIngredients(text: String): List<DetectedIngredient> {
        val ingredientKeywords = listOf(
            "făină", "zahăr", "ulei", "sare", "apă", "drojdie", "lapte", 
            "ouă", "unt", "vanilie", "cacao", "ciocolată", "miere", "grâu",
            "porumb", "orez", "smântână", "zer", "cazeină", "gluten"
        )
        
        return ingredientKeywords.filter { keyword ->
            text.lowercase().contains(keyword)
        }.map { ingredient ->
            val isAllergen = checkIfAllergen(ingredient)
            DetectedIngredient(
                text = ingredient,
                confidence = 0.85f,
                isAllergen = isAllergen
            )
        }
    }
    
    private fun extractMockAllergens(text: String): List<DetectedAllergen> {
        val allergenMap = mapOf(
            "gluten" to AllergenSeverity.HIGH,
            "grâu" to AllergenSeverity.HIGH,
            "făină" to AllergenSeverity.HIGH,
            "lapte" to AllergenSeverity.MEDIUM,
            "lactoza" to AllergenSeverity.MEDIUM,
            "cazeină" to AllergenSeverity.MEDIUM,
            "ouă" to AllergenSeverity.MEDIUM,
            "ou" to AllergenSeverity.MEDIUM,
            "soia" to AllergenSeverity.LOW,
            "nuci" to AllergenSeverity.HIGH,
            "arahide" to AllergenSeverity.HIGH,
            "susan" to AllergenSeverity.MEDIUM
        )
        
        return allergenMap.filter { (allergen, _) ->
            text.lowercase().contains(allergen)
        }.map { (allergen, severity) ->
            DetectedAllergen(name = allergen, severity = severity)
        }
    }
    
    private fun checkIfAllergen(ingredient: String): Boolean {
        val allergens = listOf("făină", "grâu", "lapte", "ouă", "ou", "soia", "nuci", "cazeină", "gluten")
        return allergens.any { ingredient.lowercase().contains(it) }
    }
}
