/*
 *     Copyright (c) 2022  Aditya Bavadekar
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.adityaamolbavadekar.pinlogsample

import android.app.Application
import com.adityaamolbavadekar.pinlog.PinLog
import com.adityaamolbavadekar.pinlog.extensions.initPinLoggerInDebugMode
import com.adityaamolbavadekar.pinlog.extensions.installPinLoggerExceptionHandler

/**
 * [Application] class for PinLog Sample app.
 * Mentioned in AndroidManifest.xml file.
 * */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        /**
         * Initialisation of [PinLog]
         * */
        initPinLoggerInDebugMode()

        /**
         * We are using this line to also Initialise PinLog Exception Handler,
         * which is useful to get crash report if a crash occurred.
         * */
        installPinLoggerExceptionHandler(toEmails = arrayOf("example@gmail.com"))
    }

}