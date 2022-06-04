/*******************************************************************************
 *   Copyright (c) 2022  Aditya Bavadekar
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/

package com.adityaamolbavadekar.pinlog.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.COLUMN_NAME_CREATED
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.COLUMN_NAME_ID
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.COLUMN_NAME_LOG
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.COLUMN_NAME_LOG_LEVEL
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.COLUMN_NAME_TAG
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.DATABASE_VERSION
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.TABLE_NAME
import com.adityaamolbavadekar.pinlog.PinLog
import com.adityaamolbavadekar.pinlog.PinLog.CLASS_TAG
import com.adityaamolbavadekar.pinlog.extensions.toApplicationLogModel
import com.adityaamolbavadekar.pinlog.extensions.toStringsList
import java.util.*

internal class ApplicationLogDatabaseHelper(c: Context) : SQLiteOpenHelper(
    c,
    TABLE_NAME,
    null,
    DATABASE_VERSION
), ApplicationLogDataSource {

    private var database: SQLiteDatabase? = null

    private fun initializePinLogsDatabase() {
        if (database == null) {
            database = this.writableDatabase
        }
    }

    /*START [SQLiteOpenHelper]*/
    override fun onCreate(db: SQLiteDatabase?) {
        ApplicationLogsContract.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        ApplicationLogsContract.onUpgrade(db, oldVersion, newVersion)
    }

    /*END [SQLiteOpenHelper]*/

    override fun getPinLogsCount(): Int {
        // Initialize SQLiteDatabase if it is null
        initializePinLogsDatabase()
        var count = 0
        try {
            if (database != null) {
                count = DatabaseUtils.queryNumEntries(database, TABLE_NAME).toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PinLog.logError(
                "PinLogTable: Exception occurred in Database while executing getCount: $e", e
            )
        }
        return count
    }

    override fun getPinLogsGroupCount(): Int {
        val c = getPinLogsCount()
        return if (c <= 0) {
            0
        } else {
            (c / 5000)
        }
    }

    override fun insertPinLog(applicationLog: ApplicationLogModel): Boolean {
        val errorCode: Long = -1
        var rowId: Long = -1
        database?.let { db ->
            val contentValues = ContentValues().apply {
                put(COLUMN_NAME_LOG, applicationLog.LOG)
                put(COLUMN_NAME_LOG_LEVEL, applicationLog.LOG_LEVEL)
                put(COLUMN_NAME_TAG, applicationLog.TAG)
                put(COLUMN_NAME_CREATED, System.currentTimeMillis().toInt())
            }

            try {
                rowId = db.insert(TABLE_NAME, null, contentValues)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                PinLog.logError(
                    "PinLogTable: Exception occurred in Database while executing insertPinLog: $e",
                    e
                )
            }
        }
        return (rowId != errorCode)
    }

    override fun deleteAllPinLogs() {
        database?.let { db ->

            try {
                db.delete(TABLE_NAME, null, null)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                PinLog.logError(
                    "PinLogTable: Exception occurred in Database while executing deleteAllPinLogs: $e",
                    e
                )
            }

        }
    }

    private fun deletePinLogs(ids: Array<Int>) {
        database?.let { db ->
            if (ids.isEmpty()) return
            try {
                val selection = "$COLUMN_NAME_ID LIKE ?"
                val selectionArgs = ids.toStringsList()
                db.delete(TABLE_NAME, selection, selectionArgs)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                PinLog.logError(
                    "PinLogTable: Exception occurred in Database while executing deletePinLogs: $e",
                    e
                )
            }

        }
    }

    override fun getPinLogs(group: Int): List<ApplicationLogModel> {
        return emptyList()//Not implemented
    }

    private fun getCursor(db: SQLiteDatabase): Cursor? {
        return db.query(
            TABLE_NAME,
            arrayOf(
                COLUMN_NAME_ID,//0
                COLUMN_NAME_LOG,//1
                COLUMN_NAME_LOG_LEVEL,//2
                COLUMN_NAME_TAG,//3
                COLUMN_NAME_CREATED//4
            ),
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    private fun getCursorForStringLogs(db: SQLiteDatabase): Cursor? {
        return db.query(
            TABLE_NAME,
            arrayOf(
                COLUMN_NAME_LOG//0
            ),
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    private fun getCursorForExpiryLogs(db: SQLiteDatabase): Cursor? {
        return db.query(
            TABLE_NAME,
            arrayOf(
                COLUMN_NAME_ID,//0
                COLUMN_NAME_CREATED//1
            ),
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    override fun getAllPinLogs(): List<ApplicationLogModel> {
        val applicationLogsList: MutableList<ApplicationLogModel> = mutableListOf()
        database?.let { db ->
            val c = getCursor(db)

            if (c == null || c.isClosed) return emptyList()
            else try {
                if (c.moveToFirst()) {
                    do {
                        if (c.isClosed) {
                            break
                        }
                        applicationLogsList.add(c.toApplicationLogModel())
                    } while (c.moveToNext())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                PinLog.logE(
                    CLASS_TAG,
                    "PinLogTable: Exception occurred in Database while executing getPinLogs: $e"
                )
            }
            c.close()
            return applicationLogsList.toList()

        }
        return applicationLogsList.toList()
    }

    override fun getAllPinLogsAsStringList(): List<String> {
        val pinLogsList: MutableList<String> = mutableListOf()
        database?.let { db ->
            val c = getCursorForStringLogs(db)

            if (c == null || c.isClosed) return emptyList()
            else try {
                if (c.moveToFirst()) {
                    do {
                        if (c.isClosed) {
                            break
                        }
                        pinLogsList.add(c.getString(0) ?: "")
                    } while (c.moveToNext())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                PinLog.logError(
                    "PinLogTable: Exception occurred in Database while executing getAllPinLogsAsStringList: $e",
                    e
                )
            }
            c.close()
            return pinLogsList.toList()

        }
        return pinLogsList.toList()
    }

    override fun deletedExpiredPinLogs(expiryTimeInSeconds: Int) {
        if (database == null) {
            return
        }
        database?.let { db ->
            val c = getCursorForExpiryLogs(db)
            val deletableLogs = mutableListOf<Int>()
            if (c == null || c.isClosed) return
            else try {
                if (c.moveToFirst()) {
                    do {
                        if (c.isClosed) break

                        val id = c.getInt(0)
                        val created: Long = c.getInt(1).toLong()
                        val today = Calendar.getInstance()
                        today.timeInMillis = System.currentTimeMillis()
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = created

                        if (cal[Calendar.DAY_OF_YEAR] + 7 < today[Calendar.DAY_OF_YEAR]) {
                            deletableLogs.add(id)
                        }

                    } while (c.moveToNext())
                }
            } catch (e: Exception) {
            }
            deletePinLogs(deletableLogs.toTypedArray())
        }
    }

    init {
        initializePinLogsDatabase()
    }

}