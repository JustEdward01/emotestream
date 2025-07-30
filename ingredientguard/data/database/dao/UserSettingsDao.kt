package com.ingredientguard.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.ingredientguard.data.models.UserSettings

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    fun getUserSettings(userId: String = "current_user"): Flow<UserSettings?>
    
    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    suspend fun getUserSettingsOnce(userId: String = "current_user"): UserSettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: UserSettings)
    
    @Update
    suspend fun updateSettings(settings: UserSettings)
}