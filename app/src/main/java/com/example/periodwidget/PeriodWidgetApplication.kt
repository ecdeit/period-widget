package com.example.periodwidget

import android.app.Application
import androidx.room.Room

class PeriodWidgetApplication : Application() {

    private var _database: PeriodDatabase? = null
    val database: PeriodDatabase
        get() {
            if (_database == null) {
                _database = Room.databaseBuilder(
                    this,
                    PeriodDatabase::class.java,
                    "period_db"
                ).fallbackToDestructiveMigration().build()
            }
            return _database!!
        }

    override fun onTerminate() {
        _database?.close()
        super.onTerminate()
    }
}