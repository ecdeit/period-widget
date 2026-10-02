package com.example.periodwidget

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(period: PeriodEntity): Long

    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    fun observePeriods(): Flow<List<PeriodEntity>>

    /** One-shot read, used by the widget worker where a Flow cannot be collected. */
    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    suspend fun getPeriodsOnce(): List<PeriodEntity>

    @Query("SELECT * FROM periods WHERE startDate = :startDate")
    suspend fun getPeriodByDate(startDate: String): PeriodEntity?

    @Query("DELETE FROM periods WHERE startDate = :startDate")
    suspend fun deletePeriod(startDate: String): Int

    @Query("DELETE FROM periods")
    suspend fun clearAllPeriods(): Int
}