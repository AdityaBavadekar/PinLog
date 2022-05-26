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

import android.content.Context
import com.adityaamolbavadekar.pinlog.PinLog

/*ERROR LOGGING*/

/**
 * **Logs an Error Log**
 *
 * **Logs to Logcat if [PinLog.setDevLogging] is set to `true`**
 *
 * **Stores the log if [PinLog.setDoStoreLogs] is set to `true`**
 *
 * @see [PinLog.logE]
 *
 * */
fun Context.logE(s: String?) {
    PinLog.logE(getName(), s ?: "")
}

/**
 * **Logs an Error Log**
 *
 * **Logs to Logcat if [PinLog.setDevLogging] is set to `true`**
 *
 * **Stores the log if [PinLog.setDoStoreLogs] is set to `true`**
 *
 * @see [PinLog.logE]
 *
 * */
fun Context.logE(s: String?, throwable: Throwable?) {
    PinLog.logE(getName(), s, throwable ?: Exception(""))
}

/*INFO LOGGING*/

/**
 * **Logs an Info Log**
 *
 * **Logs to Logcat if [PinLog.setDevLogging] is set to `true`**
 *
 * **Stores the log if [PinLog.setDoStoreLogs] is set to `true`**
 *
 * @see [PinLog.logI]
 *
 * */
fun Context.logI(s: String?) {
    PinLog.logI(getName(), s ?: "")
}

/**
 * **Logs an Info Log**
 *
 * **Logs to Logcat if [PinLog.setDevLogging] is set to `true`**
 *
 * **Stores the log if [PinLog.setDoStoreLogs] is set to `true`**
 *
 * @see [PinLog.logI]
 *
 * */
fun Context.logI(s: String?, throwable: Throwable?) {
    PinLog.logI(getName(), s, throwable ?: Exception(""))
}

/*WARNING LOGGING*/

/**
 * **Logs an Warn Log**
 *
 * **Logs to Logcat if [PinLog.setDevLogging] is set to `true`**
 *
 * **Stores the log if [PinLog.setDoStoreLogs] is set to `true`**
 *
 * @see [PinLog.logW]
 *
 * */
fun Context.logW(s: String?) {
    PinLog.logW(getName(), s ?: "")
}

/**
 * **Logs an Warn Log**
 *
 * **Logs to Logcat if [PinLog.setDevLogging] is set to `true`**
 *
 * **Stores the log if [PinLog.setDoStoreLogs] is set to `true`**
 *
 * @see [PinLog.logW]
 *
 * */
fun Context.logW(s: String?, throwable: Throwable?) {
    PinLog.logW(getName(), s, throwable ?: Exception(""))
}

/*DEBUG LOGGING*/

/**
 * **Logs an Debug Log**
 *
 * **Logs to Logcat if [PinLog.setDevLogging] is set to `true`**
 *
 * **Stores the log if [PinLog.setDoStoreLogs] is set to `true`**
 *
 * @see [PinLog.logD]
 *
 * */
fun Context.logD(s: String?) {
    PinLog.logD(getName(), s ?: "")
}

/**
 * **Logs an Debug Log**
 *
 * **Logs to Logcat if [PinLog.setDevLogging] is set to `true`**
 *
 * **Stores the log if [PinLog.setDoStoreLogs] is set to `true`**
 *
 * @see [PinLog.logD]
 *
 * */
fun Context.logD(s: String?, throwable: Throwable?) {
    PinLog.logD(getName(), s, throwable ?: Exception(""))
}


/*Others*/

private fun Context.getName(): String {
    return this.javaClass.simpleName
}