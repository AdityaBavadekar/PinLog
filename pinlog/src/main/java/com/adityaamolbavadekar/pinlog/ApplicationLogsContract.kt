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

package com.adityaamolbavadekar.pinlog

import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.DATABASE_CREATE
import com.adityaamolbavadekar.pinlog.ApplicationLogsContract.ApplicationLogEntry.TABLE_NAME

internal object ApplicationLogsContract {

    object ApplicationLogEntry : BaseColumns {
        const val DATABASE_VERSION: Int = 1
        const val TABLE_NAME: String = "pin_logger_logs"
        const val COLUMN_NAME_ID: String = "_id"
        const val COLUMN_NAME_LOG: String = "logs"
        const val COLUMN_NAME_LOG_LEVEL: String = "log_level"
        const val COLUMN_NAME_TAG: String = "log_tag"
        const val COLUMN_NAME_CREATED: String = "created"

        const val DATABASE_CREATE = ("CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME
                + " ("
                + COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME_LOG + " TEXT, "
                + COLUMN_NAME_LOG_LEVEL + " INTEGER, "
                + COLUMN_NAME_TAG + " TEXT, "
                + COLUMN_NAME_CREATED + " INTEGER"
                + ");")
    }


    fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null) {
            return
        }
        try {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
            PinLog.logInfo(
                "PinLogTable onUpgrade called. Executing drop_table query to delete old logs."
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            PinLog.logError(
                "PinLogTable: Exception occurred while executing Database in onUpgrade: $e", e
            )
        }
    }

    fun onCreate(db: SQLiteDatabase?) {
        if (db == null) return

        try {
            db.execSQL(DATABASE_CREATE)
            PinLog.logInfo(
                "PinLogTable: Successfully created PinLogs Database"
            )

        } catch (e: Exception) {
            e.printStackTrace()
            PinLog.logError(
                "PinLogTable: Exception occurred while executing Database in onCreate: $e", e
            )
        }
    }

}

