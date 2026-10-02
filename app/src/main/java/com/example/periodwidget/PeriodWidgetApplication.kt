package com.example.periodwidget

import android.app.Application
import androidx.room.Room

class PeriodWidgetApplication : Application() {

    @Volatile
    private var instance: PeriodDatabase? = null

    /**
     * Lazily built and cached. Room's own builder is already thread-safe, but the
     * reference is cached here so the widget worker and the UI never race to
     * open the same file twice.
     */
    val database: PeriodDatabase
        get() = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                applicationContext,
                PeriodDatabase::class.java,
                "period_db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
}
