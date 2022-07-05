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

import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.TextView
import androidx.core.content.edit
import com.adityaamolbavadekar.pinlog.PinLog
import com.adityaamolbavadekar.pinlog.extensions.logI
import com.adityaamolbavadekar.pinlog.utils.ui.BaseLifecycleLoggerActivity
import java.util.*

/**
 * A class demonstrating usage of commonly used [PinLog] methods and
 * also extends [BaseLifecycleLoggerActivity] from save library.
 * */
class MainActivity : BaseLifecycleLoggerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        var clicksSize = prefs.getInt("Clicks", 0)
        findViewById<TextView>(R.id.textView).setOnClickListener {
            clicksSize += 1
            prefs.edit {
                /**
                 * Store value of clicksSize in the preferences so
                 * that, even if app crashes we know the value of
                 * clicksSize as PinLog added it in the created report.
                 * */
                putInt("Clicks", clicksSize)
            }

            /**
             * Using the PinLog.CustomLogFileData method to store
             * custom value which is not needed to be saved in Preferences as
             * that value is intended for the developer (eg:understanding state of app).
             * */
            PinLog.CustomLogFileData().put("LastClicked", "${Date()}")

            /**
             * Add logs.
             * */
            logI("Hooray! TextView was clicked.")
            PinLog.logI("View", "Hooray! TextView was clicked.")
            PinLog.logD("TextView", "Hooray! TextView was clicked.")
            PinLog.logW("ClicksRecorder", "Clicks size is now $clicksSize.")
        }

        findViewById<TextView>(R.id.textView2).setOnClickListener {
            /**
             * We are not logging this report through PinLog.logI() method because
             * the Report created is 200+ lines. And there in no need to store this report
             * in the PinLog database. Instead we can use [PinLog.getAllPinLogsInFile]
             * if you want to store the logs.
             * */
            Log.d(
                javaClass.simpleName,
                PinLog.CrashReporter().createReport(Thread.currentThread(), NullPointerException())
                    .toString()
            )
        }

        findViewById<TextView>(R.id.textView3).setOnClickListener {
            PinLog.CustomLogFileData().put("CustomExceptionWasThrown", true)

            /**
             * Throw an Exception to demonstrate [PinLog.setupExceptionHandler].
             * */
            throw CustomException()
        }

    }

    class CustomException : Throwable("Custom exception created by pinlogsample.")
}
