package com.example.periodwidget

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(period: PeriodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)

    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    fun getAllPeriods(): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestPeriod(): PeriodEntity?

    @Query("SELECT * FROM periods WHERE startDate = :startDate")
    suspend fun getPeriodByDate(startDate: String): PeriodEntity?

    @Query("DELETE FROM periods WHERE startDate = :startDate")
    suspend fun deletePeriod(startDate: String): Int

    @Query("DELETE FROM periods")
    suspend fun clearAllPeriods(): Int

    @Query("SELECT * FROM settings WHERE key = 'app_settings'")
    suspend fun getSettings(): SettingsEntity?
}