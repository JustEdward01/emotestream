package com.ingredientguard.data.analyzer

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ingredientguard.data.models.*
import com.ingredientguard.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EnhancedTextAnalyzer(private val context: Context) {
    private val userRepository = UserRepository(context)

    fun analyzeImage(
        uri: Uri,
        onResult: (EnhancedAnalysisResult) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text

                    // Process text with personal allergen checking
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = processTextWithPersonalAllergens(fullText)
                            onResult(result)
                        } catch (e: Exception) {
                            onError(e)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    private suspend fun processTextWithPersonalAllergens(text: String): EnhancedAnalysisResult {
        // Extract basic ingredients and allergens (existing logic)
        val basicIngredients = extractIngredients(text)
        val detectedAllergens = extractAllergens(text)

        // Get user's personal allergens
        val personalAllergens = userRepository.checkPersonalAllergens(
            detectedAllergens.map { it.name }
        )

        // Enhance allergens with personal relevance
        val enhancedAllergens = detectedAllergens.map { allergen ->
            if (personalAllergens.contains(allergen.name.lowercase())) {
                allergen.copy(severity = AllergenSeverity.HIGH) // Elevate severity for personal allergens
            } else {
                allergen
            }
        }

        return EnhancedAnalysisResult(
            fullText = text,
            ingredients = basicIngredients,
            allergens = enhancedAllergens,
            personalAllergensDetected = personalAllergens.isNotEmpty(),
            personalAllergensList = personalAllergens
        )
    }

    private fun extractIngredients(text: String): List<DetectedIngredient> {
        val ingredientKeywords = listOf(
            "făină", "zahăr", "ulei", "sare", "apă", "drojdie", "lapte",
            "ouă", "unt", "vanilie", "cacao", "ciocolată", "miere", "grâu",
            "porumb", "orez", "smântână", "zer", "cazeină"
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

    private fun extractAllergens(text: String): List<DetectedAllergen> {
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
        val allergens = listOf("făină", "grâu", "lapte", "ouă", "ou", "soia", "nuci", "cazeină")
        return allergens.any { ingredient.lowercase().contains(it) }
    }
}