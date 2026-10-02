package com.example.periodwidget

import android.app.Application

class PeriodWidgetApplication : Application() {

    @Volatile
    private var instance: PeriodStore? = null

    /**
     * Lazily built and cached so the widget worker and the UI never open two
     * helpers against the same database file.
     */
    val store: PeriodStore
        get() = instance ?: synchronized(this) {
            instance ?: PeriodStore(applicationContext).also { instance = it }
        }
}
