package com.ingredientguard.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ingredientguard.data.repository.HistoryRepository
import com.ingredientguard.data.repository.ScanHistoryItem
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HistoryRepository(application)

    var historyItems by mutableStateOf<List<ScanHistoryItem>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var showOnlyWithAllergens by mutableStateOf(false)
        private set

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            isLoading = true
            try {
                historyItems = if (showOnlyWithAllergens) {
                    repository.getScansWithAllergens() // suspend function
                } else {
                    repository.getAllScansOnce() // suspend function, nu Flow
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    fun searchHistory(query: String) {
        searchQuery = query
        viewModelScope.launch {
            isLoading = true
            try {
                historyItems = if (query.isBlank()) {
                    if (showOnlyWithAllergens) repository.getScansWithAllergens()
                    else repository.getAllScansOnce()
                } else {
                    repository.searchScans(query)
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleAllergenFilter() {
        showOnlyWithAllergens = !showOnlyWithAllergens
        if (searchQuery.isBlank()) {
            loadHistory()
        } else {
            searchHistory(searchQuery)
        }
    }

    fun deleteScan(item: ScanHistoryItem) {
        viewModelScope.launch {
            try {
                repository.deleteScan(item)
                loadHistory() // Reload după delete
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}