package com.ingredientguard.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.ingredientguard.data.models.UserProfile

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    fun getUserProfile(userId: String = "current_user"): Flow<UserProfile?>
    
    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    suspend fun getUserProfileOnce(userId: String = "current_user"): UserProfile?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
    
    @Update
    suspend fun updateProfile(profile: UserProfile)
    
    @Query("UPDATE user_profiles SET personalAllergens = :allergens WHERE id = :userId")
    suspend fun updatePersonalAllergens(allergens: List<String>, userId: String = "current_user")
    
    @Query("UPDATE user_profiles SET isOnboardingCompleted = :completed WHERE id = :userId")
    suspend fun setOnboardingCompleted(completed: Boolean, userId: String = "current_user")
}