
package com.example.fitnesstracker.data.repository

import com.example.fitnesstracker.data.local.WeightEntry
import com.example.fitnesstracker.data.local.WeightEntryDao
import kotlinx.coroutines.flow.Flow

class WeightEntryRepository(
    private val weightEntryDao: WeightEntryDao
) {

    fun getAllEntries(): Flow<List<WeightEntry>> =
        weightEntryDao.getAllEntries()

    suspend fun getEntryForDate(dateEpochDay: Long): WeightEntry? =
        weightEntryDao.getEntryForDate(dateEpochDay)

    suspend fun insertEntry(entry: WeightEntry): Long =
        weightEntryDao.insertEntry(entry)

    suspend fun updateEntry(entry: WeightEntry) {
        weightEntryDao.updateEntry(
            entry.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteEntry(entry: WeightEntry) {
        weightEntryDao.deleteEntry(entry)
    }
}