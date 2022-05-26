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

import android.content.Context


/**
 * Created by Aditya Bavadekar on 18 May 2022
 *
 *
 * Set this property in [PinLog.setLogFormatting]. Override the [LoggingStyle.getFormattedLogData] method to implement custom logging string.
 *
 * *Default implementation is [DefaultApplicationLoggingStyle]*
 *
 * */
abstract class LoggingStyle {

    private var context:Context? =null

    constructor()

    /**
     * Called before storing log to Database.
     *
     * *Default implementation is [DefaultApplicationLoggingStyle.getFormattedLogData]*
     * */
    abstract fun getFormattedLogData(
            TAG: String,
            m: String,
            e: Throwable?,
            dateLong: Long,
            level: PinLog.LogLevel,
            VERSION_NAME: String,
            VERSION_CODE: String,
            PACKAGE_NAME: String
    ): String



}