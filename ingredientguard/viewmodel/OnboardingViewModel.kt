package com.ingredientguard.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ingredientguard.data.repository.UserRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository(application)
    
    var firstName by mutableStateOf("")
        private set
    var lastName by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var selectedAllergens by mutableStateOf<Set<String>>(emptySet())
        private set
    var currentStep by mutableStateOf(0)
        private set
    var isLoading by mutableStateOf(false)
        private set
    
    // Predefined allergens list
    val availableAllergens = listOf(
        "Gluten", "Lactoza", "Ouă", "Nuci", "Arahide", "Soia", 
        "Pește", "Crustacee", "Susan", "Muștar", "Telină", "Lupin"
    )
    
    fun updateFirstName(name: String) {
        firstName = name
    }
    
    fun updateLastName(name: String) {
        lastName = name
    }
    
    fun updateEmail(email: String) {
        this.email = email
    }
    
    fun toggleAllergen(allergen: String) {
        selectedAllergens = if (selectedAllergens.contains(allergen)) {
            selectedAllergens - allergen
        } else {
            selectedAllergens + allergen
        }
    }
    
    fun nextStep() {
        if (currentStep < 2) {
            currentStep++
        }
    }
    
    fun previousStep() {
        if (currentStep > 0) {
            currentStep--
        }
    }
    
    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                repository.initializeNewUser(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    selectedAllergens = selectedAllergens.toList()
                )
                onComplete()
            } catch (e: Exception) {
                // Handle error - could add error state here
            } finally {
                isLoading = false
            }
        }
    }
    
    fun isStepValid(): Boolean {
        return when (currentStep) {
            0 -> firstName.isNotBlank() && lastName.isNotBlank()
            1 -> email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            2 -> true // Allergen selection is optional
            else -> false
        }
    }
}