package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Einziges Nutzerprofil (id = 1). Geschlecht und Aktivität als Enum-Name gespeichert. */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = SINGLE_PROFILE_ID,
    val heightCm: Int,
    val ageYears: Int,
    val sex: String,
    val activityLevel: String,
    val proteinPercent: Int,
    val carbsPercent: Int,
    val fatPercent: Int,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val SINGLE_PROFILE_ID = 1
    }
}

/** Zugriff auf das Profil. Beim Speichern immer ALLE Felder mitgeben (REPLACE). */
@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfile)
}
