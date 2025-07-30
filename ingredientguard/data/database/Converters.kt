package com.ingredientguard.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ingredientguard.data.models.DetectedIngredient
import com.ingredientguard.data.models.DetectedAllergen
import java.util.Date

class Converters {
    private val gson = Gson()

    // Pentru UserProfile
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    // Pentru DetectedIngredient List
    @TypeConverter
    fun fromDetectedIngredientList(value: List<DetectedIngredient>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDetectedIngredientList(value: String): List<DetectedIngredient> {
        val listType = object : TypeToken<List<DetectedIngredient>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    // Pentru DetectedAllergen List
    @TypeConverter
    fun fromDetectedAllergenList(value: List<DetectedAllergen>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDetectedAllergenList(value: String): List<DetectedAllergen> {
        val listType = object : TypeToken<List<DetectedAllergen>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    // Pentru Date
    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }
}