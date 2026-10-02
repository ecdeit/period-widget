package com.example.periodwidget

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One recorded period.
 *
 * startDate is stored as ISO text rather than an epoch value so the record is
 * stable across timezone changes: a period day is a calendar day, not an
 * instant. Deleting by startDate is therefore unambiguous.
 */
@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String,   // ISO_LOCAL_DATE, e.g. 2024-01-01
    val length: Int,         // 2..10 days
    val timestamp: Long = System.currentTimeMillis()
)
