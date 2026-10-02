package com.example.periodwidget

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PeriodEntity::class, SettingsEntity::class], version = 1, exportSchema = false)
abstract class PeriodDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
}