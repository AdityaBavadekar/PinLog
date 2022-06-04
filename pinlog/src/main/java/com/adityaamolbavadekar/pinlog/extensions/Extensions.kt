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

package com.adityaamolbavadekar.pinlog.extensions

import android.app.Application
import android.database.Cursor
import com.adityaamolbavadekar.pinlog.PinLog
import com.adityaamolbavadekar.pinlog.PinLog.CLASS_TAG
import com.adityaamolbavadekar.pinlog.database.ApplicationLogModel

fun Application.initPinLogger(): Boolean {
    return PinLog.initialise(this)
}

fun Application.initPinLoggerInDebugMode(): Boolean {
    return PinLog.initialiseDebug(this)
}

fun Application.initPinLoggerInReleaseMode(): Boolean {
    return PinLog.initialiseRelease(this)
}

inline fun debug(m: () -> String) {
    PinLog.logD("?", m.invoke())
}

inline fun warn(m: () -> String) {
    PinLog.logW("?", m.invoke())
}

inline fun info(m: () -> String) {
    PinLog.logI("?", m.invoke())
}

inline fun error(m: () -> String) {
    PinLog.logE("?", m.invoke())
}

inline fun error(e: Exception, m: () -> String) {
    PinLog.logE("?", m.invoke(), e)
}

fun MutableList<PinLog.OnStringLogAddedListener>.submitLog(log: String) {
    for (it in this) {
        try {
            it.onLogAdded(log)
        } catch (e: Exception) {
            PinLog.logW(CLASS_TAG, "Could not notify $it about newly added log")
        }
    }
}

fun MutableList<PinLog.OnLogAddedListener>.submitLog(log: ApplicationLogModel) {
    for (it in this) {
        try {
            it.onLogAdded(log)
        } catch (e: Exception) {
            PinLog.logW(CLASS_TAG, "Could not notify $it about newly added log")
        }
    }
}

internal fun Cursor.toApplicationLogModel(): ApplicationLogModel {
    val id: Int = getInt(0)
    val log: String = getString(1)
    val level: Int = getInt(2)
    val tag: String = getString(3) ?: ""
    val created: Long = getInt(4).toLong()
    return ApplicationLogModel(id, log, level, tag, created)
}

fun Array<Int>.toStringsList(): Array<String> {
    val list = arrayListOf<String>()
    this.forEach {
        list.add("$it")
    }
    return list.toTypedArray()
}

fun <T> MutableList<T>.clearListeners() {
    if (this.isEmpty()) return
    while (this.size != 0) {
        this.removeAt(this.size - 1)
    }
}