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

package com.adityaamolbavadekar.pinlogactivity

import android.app.Application
import androidx.annotation.NonNull
import com.adityaamolbavadekar.pinlog.PinLog


object PinLogActivityHolder {

    /**
     * Initialises [PinLog] from [PinLogActivityHolder]
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    @JvmStatic
    fun initialise(
        @NonNull application: Application,
        setDevLoggingEnabled: Boolean = true,
        setDoStoreLogs: Boolean = true,
        buildConfig: Class<*>? = null
    ): Boolean = PinLog.initialise(application, setDevLoggingEnabled, setDoStoreLogs, buildConfig)

    internal fun getPinLogClass()= PinLog

}