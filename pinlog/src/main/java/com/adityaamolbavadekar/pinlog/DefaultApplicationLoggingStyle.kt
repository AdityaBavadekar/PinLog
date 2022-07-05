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

import java.util.*

/**
 *
 * ```
 * Example :
 * "Vr[1.0] 05-17 20:04:11.603 18494-18629/? D/PinLog: Hello World from PinLog"
 *
 * ```
 *
 * */
class DefaultApplicationLoggingStyle : LoggingStyle() {

    override fun getFormattedLogData(
        TAG: String,
        m: String,
        e: Throwable?,
        dateLong: Long,
        level: PinLog.LogLevel,
        VERSION_NAME: String,
        VERSION_CODE: String,
        PACKAGE_NAME: String
    ): String {
        var string =
            "Vr/[${VERSION_NAME}] " + "${Date(dateLong)}" + "/ " + "${level.SHORT_NAME}/" + "$TAG : " + m
        if (e != null) {
            string += "\n" + PinLog.getStackTraceString(e)
        }
        return string
    }
}