package com.ingredientguard.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ingredientguard.data.repository.UserRepository
import com.ingredientguard.data.models.UserSettings
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository(application)
    
    var settings by mutableStateOf<UserSettings?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            repository.getUserSettings().collect { userSettings ->
                settings = userSettings
            }
        }
    }
    
    fun updateNotifications(enabled: Boolean) {
        updateSetting { it.copy(notificationsEnabled = enabled) }
    }
    
    fun updateSound(enabled: Boolean) {
        updateSetting { it.copy(soundEnabled = enabled) }
    }
    
    fun updateVibration(enabled: Boolean) {
        updateSetting { it.copy(vibrationEnabled = enabled) }
    }
    
    fun updateAutoSave(enabled: Boolean) {
        updateSetting { it.copy(autoSaveScans = enabled) }
    }
    
    fun updateWarningThreshold(threshold: String) {
        updateSetting { it.copy(warningThreshold = threshold) }
    }
    
    fun updateTheme(theme: String) {
        updateSetting { it.copy(theme = theme) }
    }
    
    private fun updateSetting(update: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            settings?.let { currentSettings ->
                val updatedSettings = update(currentSettings)
                repository.updateSettings(updatedSettings)
            }
        }
    }
}