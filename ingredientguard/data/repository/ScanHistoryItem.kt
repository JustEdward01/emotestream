package com.ingredientguard.data.repository

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ingredientguard.data.database.Converters
import com.ingredientguard.data.models.DetectedIngredient
import com.ingredientguard.data.models.DetectedAllergen
import java.util.Date

@Entity(tableName = "scan_history")
@TypeConverters(Converters::class)
data class ScanHistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Date,
    val fullText: String,
    val imagePath: String? = null,
    val ingredients: List<DetectedIngredient> = emptyList(),
    val allergens: List<DetectedAllergen> = emptyList(),
    val hasAllergens: Boolean = false,
    // Enhanced fields
    val hasPersonalAllergens: Boolean = false,
    val personalAllergensFound: List<String> = emptyList()
)