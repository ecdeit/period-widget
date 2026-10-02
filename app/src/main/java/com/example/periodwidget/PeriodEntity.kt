package com.example.periodwidget

/**
 * One recorded period.
 *
 * startDate is ISO text rather than an epoch value so the record is stable
 * across timezone changes: a period day is a calendar day, not an instant.
 */
data class PeriodEntity(
    val id: Long = 0,
    val startDate: String,   // ISO_LOCAL_DATE, e.g. 2024-01-01
    val length: Int,         // 2..10 days
    val timestamp: Long = System.currentTimeMillis()
)
