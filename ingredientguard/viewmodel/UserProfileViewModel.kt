package com.ingredientguard.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ingredientguard.data.repository.UserRepository
import com.ingredientguard.data.models.UserProfile
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
)

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository(application)
    
    var uiState by mutableStateOf(ProfileUiState())
        private set
    
    init {
        loadProfile()
    }
    
    private fun loadProfile() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                repository.getUserProfile().collect { profile ->
                    uiState = uiState.copy(
                        profile = profile,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun updateProfile(updatedProfile: UserProfile) {
        viewModelScope.launch {
            try {
                repository.createOrUpdateProfile(updatedProfile)
                uiState = uiState.copy(isEditing = false)
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }
    
    fun updatePersonalAllergens(allergens: List<String>) {
        viewModelScope.launch {
            try {
                repository.updatePersonalAllergens(allergens)
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }
    
    fun startEditing() {
        uiState = uiState.copy(isEditing = true)
    }
    
    fun cancelEditing() {
        uiState = uiState.copy(isEditing = false)
    }
    
    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}