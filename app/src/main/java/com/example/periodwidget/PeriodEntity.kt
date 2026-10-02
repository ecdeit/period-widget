package com.example.periodwidget

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String,           // YYYY-MM-DD
    val length: Int,                 // 2-10 days
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String = "app_settings",
    val remindersEnabled: Boolean = true,
    val reminderDaysBefore: Int = 2,
    val theme: String = "system",    // light, dark, system
    val firstDayOfWeek: Int = 0      // 0=Sunday, 1=Monday
)