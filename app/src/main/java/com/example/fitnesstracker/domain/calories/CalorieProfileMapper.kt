package com.example.fitnesstracker.domain.calories

import com.example.fitnesstracker.data.local.UserProfile

/** Room-Profil -> Domain-Profil. Null bei unlesbaren Enum-Werten. */
fun UserProfile.toCalorieProfile(): CalorieProfile? {
    val s = runCatching { Sex.valueOf(sex) }.getOrNull() ?: return null
    val a = runCatching { ActivityLevel.valueOf(activityLevel) }.getOrNull() ?: return null
    return CalorieProfile(heightCm, ageYears, s, a, MacroSplit(proteinPercent, carbsPercent, fatPercent))
}

/** Domain-Profil -> Room-Profil (id = 1, alle Felder gesetzt). */
fun CalorieProfile.toEntity(createdAt: Long, updatedAt: Long): UserProfile = UserProfile(
    heightCm = heightCm,
    ageYears = ageYears,
    sex = sex.name,
    activityLevel = activity.name,
    proteinPercent = split.proteinPercent,
    carbsPercent = split.carbsPercent,
    fatPercent = split.fatPercent,
    createdAt = createdAt,
    updatedAt = updatedAt
)
