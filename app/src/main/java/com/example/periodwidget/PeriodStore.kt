package com.example.periodwidget

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local storage for period records.
 *
 * A hand-written SQLiteOpenHelper rather than Room: the schema is a single
 * table, and Room would pull in an annotation processor (kapt/KSP) whose plugin
 * resolution and Kotlin-version alignment are extra build failure modes for no
 * benefit at this size.
 *
 * startDate carries a UNIQUE constraint so recording the same start date twice
 * updates that row instead of creating a duplicate.
 */
class PeriodStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID      INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_START   TEXT    NOT NULL UNIQUE,
                $COL_LENGTH  INTEGER NOT NULL,
                $COL_TS      INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Only one schema revision exists so far; a destructive reset is the
        // honest behaviour until a real migration is needed.
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    /** Most recent first, which is the order the UI and predictions want. */
    fun all(): List<PeriodEntity> {
        val out = ArrayList<PeriodEntity>()
        readableDatabase.query(
            TABLE, null, null, null, null, null, "$COL_START DESC"
        ).use { c ->
            val iId = c.getColumnIndexOrThrow(COL_ID)
            val iStart = c.getColumnIndexOrThrow(COL_START)
            val iLen = c.getColumnIndexOrThrow(COL_LENGTH)
            val iTs = c.getColumnIndexOrThrow(COL_TS)
            while (c.moveToNext()) {
                out.add(
                    PeriodEntity(
                        id = c.getLong(iId),
                        startDate = c.getString(iStart),
                        length = c.getInt(iLen),
                        timestamp = c.getLong(iTs)
                    )
                )
            }
        }
        return out
    }

    fun upsert(startDate: String, length: Int) {
        val values = ContentValues().apply {
            put(COL_START, startDate)
            put(COL_LENGTH, length)
            put(COL_TS, System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun delete(startDate: String): Int =
        writableDatabase.delete(TABLE, "$COL_START = ?", arrayOf(startDate))

    fun clear(): Int = writableDatabase.delete(TABLE, null, null)

    private companion object {
        const val DB_NAME = "period_store.db"
        const val DB_VERSION = 1
        const val TABLE = "periods"
        const val COL_ID = "id"
        const val COL_START = "start_date"
        const val COL_LENGTH = "length"
        const val COL_TS = "recorded_at"
    }
}
