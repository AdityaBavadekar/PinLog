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

@file:Suppress("unused")

package com.adityaamolbavadekar.pinlog

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.adityaamolbavadekar.pinlog.database.ApplicationLogDatabaseHelper
import com.adityaamolbavadekar.pinlog.database.ApplicationLogModel
import com.adityaamolbavadekar.pinlog.extensions.clearListeners
import com.adityaamolbavadekar.pinlog.extensions.submitLog
import com.adityaamolbavadekar.pinlog.installation.Installation
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.exitProcess


/**
 * Created by Aditya Bavadekar on 18 May 2022
 *
 * **About** :
 *
 * PinLog is an easy-to-use android Logging Library.
 * PinLog supports storing logs for later retrieval, saving logs in a file, saving logs in a zip file and more.
 * It is made by Aditya Bavadekar and written in Kotlin Language.
 * The library is Open-Source with its LICENCE bieng Apache 2.0.
 *
 * **Usage**:
 * - [PinLog] should be initialised in the [Application] Class :
 * ```
 * PinLog.initialise(this)
 * PinLog.setDevLogging(true)
 * PinLog.setBuildConfigClass(BuildConfig::class.java)
 * ```
 * *OR*
 * ```
 * //For Debuggable Builds
 * PinLog.initialiseDebug(this@App)
 *
 * //For Release Builds
 * PinLog.initialiseRelease(this@App)
 * ```
 * - *Logging*
 * Methods Available :
 * 1. [PinLog.logE]
 * 2. [PinLog.logW]
 * 3. [PinLog.logI]
 * 4. [PinLog.logD]
 *
 * You can also use these methods directly
 * in classes which extend ContextWrapper (You dont have to add TAG property) like this :
 * ```
 *
 *   class MainActivity : AppCompatActivity() {
 *       override fun onCreate(savedInstanceState: Bundle?) {
 *           super.onCreate(savedInstanceState)
 *           setContentView(R.layout.activity_main)
 *
 *           //like here no TAG is required
 *           logI("onCreate")
 *
 *       }
 *   }
 * ```
 *
 *
 * - You can get Logs stored in database by [PinLog.getAllPinLogs] method.
 * *Warning : This may return null or empty list if you've set [PinLog.setDevLogging] to `false`*
 *
 * @author Aditya Bavadekar
 * @since 19 May 2022
 *
 * */
object PinLog {

    private var isDevLoggingEnabled = false
    private var buildConfigClass: Class<*>? = null
    private var buildConfigClassData: JSONObject? = null
    private var shouldStoreLogs: Boolean = true
    private var isInitialised: Boolean = false
    private var app: Application? = null
    private var applicationLogDataSource: ApplicationLogDatabaseHelper? = null
    private var pinLoggerService: ExecutorService? = null
    private var loggingStyle: LoggingStyle? = null
    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private var customData: JSONObject = JSONObject()
    private var exceptionHandlerEnabled: Boolean = false
    private var crashEmailTo: Array<String> = arrayOf()
    private var crashMessage: String? = null
    private var crashSubject: String? = null
    private var crashMaxBuffer: Boolean = false
    private var stringLogListeners: MutableList<OnStringLogAddedListener> = mutableListOf()
    private var logListeners: MutableList<OnLogAddedListener> = mutableListOf()
    private var callerPackageName: String = ""
    private var callerVersionName: String = ""
    private var callerVersionCode: String = ""
    private var callerAppName: String = ""

    private fun instance(@NonNull application: Application): Boolean {
        synchronized(PinLog::class.java) {
            return if (app == null) {
                app = application
                isInitialised = true
                onInitialisation()
                true
            } else {
                notInitialised()
                false
            }
        }
    }

    private fun onInitialisation() {
        logInfo("Dev Logging is enabled")
        Installation.id(getContext())
        getAppInfo()
        logInfo("PinLog was initialised successfully for $callerAppName[${callerPackageName}] from ${getContext().javaClass.simpleName}")
        if (applicationLogDataSource == null) {
            applicationLogDataSource = ApplicationLogDatabaseHelper(getContext())
        }
        collectBuildConfigData()
    }

    private fun getContext(): Context {
        return app!!.applicationContext
    }

    private fun getAppInfo() {
        val packageInfo = getContext().packageManager.getPackageInfo(getContext().packageName, 0)
        callerVersionName = packageInfo.versionName
        callerVersionCode = "${PackageInfoCompat.getLongVersionCode(packageInfo)}"
        callerPackageName = packageInfo.packageName
        callerAppName =
            packageInfo.applicationInfo.loadLabel(getContext().packageManager) as String
    }

    internal fun logError(m: String, e: Exception? = null) {
        logE(CLASS_TAG, m, e)
    }

    internal fun logWarning(m: String) {
        logW(CLASS_TAG, m)
    }

    internal fun logInfo(m: String) {
        logI(CLASS_TAG, m)
    }

    private fun notInitialised() =
        logWarning("Could not initialise PinLog as it was previously initialised for $callerAppName[${callerPackageName}] from ${getContext().javaClass.simpleName}")

    private fun isAppInitialised(): Boolean {
        return when {
            app == null -> {
                logWarning("Context is null. PinLog is not initialised a method was called before initialising PinLog")
                false
            }
            applicationLogDataSource == null -> {
                logWarning("Database is not created yet!")
                false
            }
            else -> true
        }
    }


    /*Public Methods*/

    /**
     * Provided [LogLevel] it logs to the specific function.
     *
     * */
    @JvmStatic
    fun log(TAG: String, m: String, logLevel: LogLevel = LogLevel.INFO) {
        when (logLevel) {
            LogLevel.INFO -> logI(TAG, m)
            LogLevel.WARN -> logW(TAG, m)
            LogLevel.ERROR -> logE(TAG, m)
            LogLevel.DEBUG -> logD(TAG, m)
        }
    }

    /**
     * Provided [LogLevel] it logs to the specific function.
     *
     * */
    @JvmStatic
    fun log(TAG: String, m: String, e: Throwable?, logLevel: LogLevel = LogLevel.INFO) {
        when (logLevel) {
            LogLevel.INFO -> logI(TAG, m, e)
            LogLevel.WARN -> logW(TAG, m, e)
            LogLevel.ERROR -> logE(TAG, m, e)
            LogLevel.DEBUG -> logD(TAG, m, e)
        }
    }

    /**
     * @return [String] Creates a log and returns it in form of a string.
     * */
    @JvmStatic
    fun getLogAsString(
        TAG: String,
        m: String,
        e: Throwable?,
        logLevel: LogLevel = LogLevel.INFO
    ): String? {
        if (!isAppInitialised()) {
            return null
        }
        return loggingStyle!!.getFormattedLogData(
            TAG, m, e, System.currentTimeMillis(), logLevel,
            callerVersionName, callerVersionCode, callerPackageName
        )
    }

    /*ERROR*/

    /**
     * **Logs an Error Log**
     *
     * *Logs to Logcat if [PinLog.setDevLogging] is set to `true`*
     *
     * *Stores the log if [PinLog.setDoStoreLogs] is set to `true`*
     *
     *
     * */
    @JvmStatic
    fun logE(TAG: String, m: String) {
        if (isDevLoggingEnabled) Log.e(TAG, m)
        storeLog(LogData(TAG, m, null, LogLevel.ERROR))
    }

    /**
     * **Logs an Error Log**
     *
     * *Logs to Logcat if [PinLog.setDevLogging] is set to `true`*
     *
     * *Stores the log if [PinLog.setDoStoreLogs] is set to `true`*
     *
     *
     * */
    @JvmStatic
    fun logE(TAG: String, m: String?, e: Throwable?) {
        if (isDevLoggingEnabled) Log.e(TAG, m, e)
        storeLog(LogData(TAG, m, e, LogLevel.ERROR))
    }

    /*WARN*/

    /**
     * **Logs a Warning Log**
     *
     * *Logs to Logcat if [PinLog.setDevLogging] is set to `true`*
     *
     * *Stores the log if [PinLog.setDoStoreLogs] is set to `true`*
     *
     *
     * */
    @JvmStatic
    fun logW(TAG: String, m: String?, e: Throwable?) {
        if (isDevLoggingEnabled) Log.w(TAG, m, e)
        storeLog(LogData(TAG, m, e, LogLevel.WARN))
    }

    /**
     * **Logs a Warning Log**
     *
     * *Logs to Logcat if [PinLog.setDevLogging] is set to `true`*
     *
     * *Stores the log if [PinLog.setDoStoreLogs] is set to `true`*
     *
     *
     * */
    @JvmStatic
    fun logW(TAG: String, m: String) {
        if (isDevLoggingEnabled) Log.w(TAG, m)
        storeLog(LogData(TAG, m, null, LogLevel.WARN))
    }

    /*INFO*/

    /**
     * **Logs an Info Log**
     *
     * *Logs to Logcat if [PinLog.setDevLogging] is set to `true`*
     *
     * *Stores the log if [PinLog.setDoStoreLogs] is set to `true`*
     *
     *
     * */
    @JvmStatic
    fun logI(TAG: String, m: String?, e: Throwable?) {
        if (isDevLoggingEnabled) Log.i(TAG, m, e)

        storeLog(LogData(TAG, m, e, LogLevel.INFO))
    }

    /**
     * **Logs an Info Log**
     *
     * *Logs to Logcat if [PinLog.setDevLogging] is set to `true`*
     *
     * *Stores the log if [PinLog.setDoStoreLogs] is set to `true`*
     *
     *
     * */
    @JvmStatic
    fun logI(TAG: String, m: String) {
        if (isDevLoggingEnabled) Log.i(TAG, m)
        storeLog(LogData(TAG, m, null, LogLevel.INFO))
    }

    /*DEBUG*/

    /**
     * **Logs a Debug Log**
     *
     * *Logs to Logcat if [PinLog.setDevLogging] is set to `true`*
     *
     * *Stores the log if [PinLog.setDoStoreLogs] is set to `true`*
     *
     *
     * */
    @JvmStatic
    fun logD(TAG: String, m: String?, e: Throwable?) {
        if (isDevLoggingEnabled) Log.d(TAG, m, e)
        storeLog(LogData(TAG, m, e, LogLevel.DEBUG))
    }

    /**
     * **Logs a Debug Log**
     *
     * *Logs to Logcat if [PinLog.setDevLogging] is set to `true`*
     *
     * *Stores the log if [PinLog.setDoStoreLogs] is set to `true`*
     *
     *
     * */
    @JvmStatic
    fun logD(TAG: String, m: String) {
        if (isDevLoggingEnabled) Log.d(TAG, m)
        storeLog(LogData(TAG, m, null, LogLevel.DEBUG))
    }

    /*STACKTRACE*/
    /**
     * **Returns Stack-Trace for the provided [Throwable]**
     *
     * */
    @JvmStatic
    fun getStackTraceString(tr: Throwable?): String {
        return Log.getStackTraceString(tr)
    }

    private fun storeLog(data: LogData) {
        //Filter PinLog Class's logs as they are intended only for Logcat logging
        when {
            data.TAG == CLASS_TAG -> return
            !isAppInitialised() -> {
                return
            }
            else -> {
                val stringLog = loggingStyle!!.getFormattedLogData(
                    data.TAG, data.m ?: "",
                    data.e,
                    data.dateLong,
                    data.level,
                    callerVersionName,
                    callerVersionCode,
                    callerPackageName
                )
                val log = ApplicationLogModel(
                    id = 0,
                    LOG = stringLog,
                    LOG_LEVEL = data.level.LEVEL_INT,
                    TAG = data.TAG,
                    created = System.currentTimeMillis()
                )

                if (pinLoggerService == null) {
                    pinLoggerService = Executors.newSingleThreadExecutor()
                }

                if (shouldStoreLogs) {
                    val runnable = Runnable {
                        try {
                            applicationLogDataSource?.insertPinLog(log)
                        } catch (ex: java.lang.Exception) {
                            ex.printStackTrace()
                        }
                    }
                    pinLoggerService?.submit(runnable)
                }

                stringLogListeners.submitLog(stringLog)
                logListeners.submitLog(log)
            }
        }
    }

    enum class LogLevel(val LEVEL_INT: Int, val SHORT_NAME: String) {
        ERROR(0, "E"),
        WARN(1, "W"),
        INFO(2, "I"),
        DEBUG(3, "D")
    }

    /**
     * Listener which notifies when a new log is added
     * and returs the log in [onLogAdded] as a [String].
     * @see OnLogAddedListener
     * */
    interface OnStringLogAddedListener {
        fun onLogAdded(log: String)
    }


    /**
     * Listener which notifies when a new log is added
     * and returs the log in [onLogAdded] as a [ApplicationLogModel].
     * @see OnStringLogAddedListener
     * */
    interface OnLogAddedListener {
        fun onLogAdded(log: ApplicationLogModel)
    }


    /**
     * Call this to add a [OnStringLogAddedListener] which notifies you when a new log is added
     * @return [Boolean] whether listener was added.
     *
     * */
    @JvmStatic
    fun addListener(onStringLogAddedListener: OnStringLogAddedListener): Boolean {
        return if (stringLogListeners.size >= 5) {
            logWarning("Could not add new listener as max limit(5) has reached")
            false
        } else {
            stringLogListeners.add(onStringLogAddedListener)
            true
        }
    }

    /**
     * Call this to remove a previously added [OnStringLogAddedListener]
     * @return [Boolean] whether listener was removed.
     *
     * */
    @JvmStatic
    fun removeListener(onStringLogAddedListener: OnStringLogAddedListener): Boolean {
        return if (!stringLogListeners.contains(onStringLogAddedListener)) {
            logWarning("Could not remove listener as it was not found")
            false
        } else {
            stringLogListeners.remove(onStringLogAddedListener)
            true
        }
    }


    /**
     * Call this to add a [OnLogAddedListener] which notifies you when a new log is added
     * @return [Boolean] whether listener was added.
     *
     * */
    @JvmStatic
    fun addListener(onLogAddedListener: OnLogAddedListener): Boolean {
        return if (stringLogListeners.size >= 5) {
            logWarning("Could not add new listener as max limit(5) has reached")
            false
        } else {
            logListeners.add(onLogAddedListener)
            true
        }
    }

    /**
     * Call this to remove a previously added [OnLogAddedListener]
     * @return [Boolean] whether listener was removed.
     *
     * */
    @JvmStatic
    fun removeListener(onLogAddedListener: OnLogAddedListener): Boolean {
        return if (!logListeners.contains(onLogAddedListener)) {
            logWarning("Could not remove listener as it was not found")
            false
        } else {
            logListeners.remove(onLogAddedListener)
            true
        }
    }

    /**
     * Call this to remove all [OnLogAddedListener] and [OnStringLogAddedListener].
     * */
    @JvmStatic
    fun removeAllListeners() {
        stringLogListeners.clearListeners()
        logListeners.clearListeners()
    }


    /**
     *
     * @return [Int] Total Count of Logs that are stored in PinLog's database
     * May return 0 if PinLog was not initialised.
     *
     * */
    @JvmStatic
    fun getPinLogsCount(): Int {
        return when {
            !isAppInitialised() -> {
                0
            }
            else -> {
                applicationLogDataSource!!.getPinLogsCount()
            }
        }
    }

    /**
     * Call this to Get Logs
     *
     * *Generally you should call this method in
     * a CoroutineJob or another Thread so it does not block main-ui thread*
     *
     * @return List of all logs that are stored in PinLog's database
     * May return emptyList if PinLog was not initialised.
     *
     * */
    @JvmStatic
    fun getAllPinLogs(): List<ApplicationLogModel> {
        return getAllPinLogs(false, 0)
    }


    /**
     * Call this to Get Logs
     *
     * *Generally you should call this method in
     * a CoroutineJob or another Thread so it does not block main-ui thread*
     *
     * @param maxLogsLimit The max number of logs that should be returned.
     * By default all logs are returned.
     * @return List of all logs that are stored in PinLog's database
     * May return emptyList if PinLog was not initialised.
     *
     * */
    @JvmStatic
    fun getAllPinLogs(maxLogsLimit: Int): List<ApplicationLogModel> {
        return getAllPinLogs(false, maxLogsLimit)
    }

    /**
     * Call this to Get Logs
     *
     * *Generally you should call this method in
     * a CoroutineJob or another Thread so it does not block main-ui thread*
     *
     * @param shouldDeleteExistingLogs Whether the logs returned should be deleted from
     * the database. *By Default logs are deleted.*
     * @param maxLogsLimit The max number of logs that should be returned.
     * By default all logs are returned.
     * @return List of all logs that are stored in PinLog's database
     * May return emptyList if PinLog was not initialised.
     *
     * */
    @JvmStatic
    fun getAllPinLogs(
        shouldDeleteExistingLogs: Boolean = true,
        maxLogsLimit: Int
    ): List<ApplicationLogModel> {
        val logs: MutableList<ApplicationLogModel> = mutableListOf()

        if (!isAppInitialised()) {
            return emptyList()
        }

        if (applicationLogDataSource != null && app != null) {
            logs.addAll(applicationLogDataSource!!.getAllPinLogs())
            if (shouldDeleteExistingLogs) deleteAllPinLogs()
        }

        if (maxLogsLimit >= 1 && logs.size > maxLogsLimit) {
            return logs.dropLast(logs.size - maxLogsLimit).toMutableList()
        }

        return logs
    }

    /**
     * Call this to Get Logs as a String List
     *
     * *Generally you should call this method in
     * a CoroutineJob or another Thread so it does not block main-ui thread*
     *
     * @return List of all logs that are stored in PinLog's database
     * May return emptyList if PinLog was not initialised.
     *
     * */
    @JvmStatic
    fun getAllPinLogsAsStringList(): List<String> {
        return getAllPinLogsAsStringList(false)
    }

    /**
     * Call this to Get Logs as a String List
     *
     * *Generally you should call this method in
     * a CoroutineJob or another Thread so it does not block main-ui thread*
     *
     * @param shouldDeleteExistingLogs Whether the logs returned should be deleted from
     * the database. *By Default logs are deleted.*
     * @return List of all logs that are stored in PinLog's database
     * May return emptyList if PinLog was not initialised.
     *
     * */
    @JvmStatic
    fun getAllPinLogsAsStringList(shouldDeleteExistingLogs: Boolean = true): List<String> {
        val logs: MutableList<String> = mutableListOf()

        if (!isAppInitialised()) {
            return emptyList()
        }

        if (applicationLogDataSource != null) {
            logs.addAll(applicationLogDataSource!!.getAllPinLogsAsStringList())
            if (shouldDeleteExistingLogs) deleteAllPinLogs()
        }
        return logs
    }

    /**
     * Call this to Get Logs as a single String.
     *
     * *Generally you should call this method in
     * a CoroutineJob or another Thread so it does not block main-ui thread*
     *
     * @return String containing all logs that are stored in PinLog's database.
     * The log element is appended by \n. May return `null` if PinLog was not initialised
     *
     *
     * */
    @JvmStatic
    fun getAllPinLogsAsString(): String? {
        var logs: String?
        if (!isAppInitialised()) {
            return null
        } else {
            val list = (applicationLogDataSource!!.getAllPinLogsAsStringList())
            logs = ""
            list.forEach {
                logs += it + "\n"
            }
        }
        return logs
    }

    /**
     * Call this to Get all saved Logs in a File.
     *
     *
     * @return A (.txt) File that contains all logs or
     * null if [PinLog] was not initialised or if some exception occured
     * while creating that file.
     *
     * */
    fun getAllPinLogsInFileWithName(fileName: String?): File? {
        return getAllPinLogsInFile(fileName = fileName, extraEndingLine = null)
    }

    /**
     * Call this to Get all saved Logs in a File.
     *
     *
     * @param extraEndingLine An extra line (String) that you want to add at the last line of file like
     * *userIdentifier or sharedPrefsData or anything.*
     *
     * Pass null if not interested.
     *
     * @return A (.txt) File that contains all logs or
     * null if [PinLog] was not initialised or if some exception occured
     * while creating that file.
     *
     * */
    fun getAllPinLogsInFile(extraEndingLine: String? = null): File? {
        return getAllPinLogsInFile(fileName = null, extraEndingLine = extraEndingLine)
    }

    /**
     * Call this to Get all saved Logs in a File.
     *
     *
     * @param fileName a File Name. Default is
     * "`{YOUR_APP_PACKAGE_NAME}_LOG_FILE_{DATE}`"
     *
     * @param extraEndingLine An extra line (String) that you want to add at the last line of file like
     * *userIdentifier or sharedPrefsData or anything.*
     *
     * Pass null if not interested.
     *
     * @return A (.txt) File that contains all logs or
     * null if [PinLog] was not initialised or if some exception occured
     * while creating that file.
     *
     * */
    @SuppressLint("SetWorldReadable")
    @JvmStatic
    fun getAllPinLogsInFile(fileName: String?, extraEndingLine: String?): File? {
        if (!isAppInitialised()) {
            return null
        }

        var logsList = getAllPinLogsAsString()

        if (logsList != null) {

            try {
                //Create a directory if it doesn't already exist.
                val filePath = getLogFilesDir()
                if (!filePath.exists()) {
                    if (!filePath.mkdir()) {
                        logWarning("Error occurred while creating directory for log file.")
                        return null
                    }
                }

                //Create a new file with file name
                val logsFile = File(filePath, fileName ?: getFileName())
                logsFile.createNewFile()

                extraEndingLine?.let { logsList = logsList!! + "\n" + it }
                logsFile.writeText(logsList!!)
                logsFile.setReadable(true, false)
                logInfo("logs were written to file : ${logsFile.name}")
                logInfo("The logs are save in a file - ${logsFile.absolutePath}")

                return logsFile
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * @return Uri for the provided file if it exists in PinLogs logs dir.
     * */
    fun getUriForFile(file: File): Uri? {
        return try {
            FileProvider.getUriForFile(getContext(), getAuthority(), file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
    }

    /**
     *
     * Call this to **delete all** of the logs stored in PinLog's database
     *
     * */
    @JvmStatic
    fun deleteAllPinLogs() {
        if (!isAppInitialised()) {
            return
        }
        if (applicationLogDataSource == null) return
        else {
            logWarning("Delete Logs request was called")
            applicationLogDataSource!!.deleteAllPinLogs()
        }
    }

    /**
     * if set to `true` Enables dev logs
     *
     * Uses built-in Android [Log] util to log to Logcat
     * *Defaults to `false`
     *
     * @param enabled Whether Dev Logs are enabled defaults to `false`
     * */
    fun setDevLogging(enabled: Boolean) {
        isDevLoggingEnabled = enabled
    }


    /**
     * Returns whether DevLogging is set to true
     * */
    fun getIsDevLoggingMode() = isDevLoggingEnabled


    /**
     * Uses built-in room to store logs
     * *Defaults to `true`
     *
     * *Note : Logs are stored for max time-period of seven days of logging.
     * After that they are deleted by [PinLog] service.*
     *
     * @param boolean
     * n Whether to store the logs
     *
     * */
    fun setDoStoreLogs(boolean: Boolean) {
        shouldStoreLogs = boolean
    }

    /**
     * Useful if you want build info in pre-logs
     * @param buildConfig The BuildConfig generated by gradle
     * */
    @JvmStatic
    fun setBuildConfigClass(buildConfig: Class<*>) {
        buildConfigClass = buildConfig
    }

    /**
     * Initialises [PinLog]
     * Defaults :
     * - [isDevLoggingEnabled]  = `false`
     * - [shouldStoreLogs] = `true`
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    @JvmStatic
    fun initialise(@NonNull application: Application): Boolean {
        return initialise(
            application = application,
            setDevLoggingEnabled = false,
            setDoStoreLogs = true,
            buildConfig = null
        )
    }

    /**
     * Initialises [PinLog]
     *
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    @JvmStatic
    fun initialise(@NonNull application: Application, setDevLoggingEnabled: Boolean): Boolean {
        return initialise(
            application = application,
            setDevLoggingEnabled = setDevLoggingEnabled,
            setDoStoreLogs = true,
            buildConfig = null
        )
    }

    /**
     * Initialises [PinLog]
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    @JvmStatic
    fun initialise(
        @NonNull application: Application,
        setDevLoggingEnabled: Boolean,
        buildConfig: Class<*>?
    ): Boolean {
        return initialise(
            application = application,
            setDevLoggingEnabled = setDevLoggingEnabled,
            setDoStoreLogs = true,
            buildConfig = buildConfig
        )
    }

    /**
     * Initialises [PinLog]
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    @JvmStatic
    fun initialise(
        @NonNull application: Application,
        setDevLoggingEnabled: Boolean,
        setDoStoreLogs: Boolean,
        buildConfig: Class<*>?
    ): Boolean {
        if (loggingStyle == null) {
            loggingStyle = DefaultApplicationLoggingStyle()
        }
        setDevLogging(setDevLoggingEnabled)
        setDoStoreLogs(setDoStoreLogs)
        buildConfig?.let { setBuildConfigClass(it) }
        return instance(application)
    }


    /**
     * Uses built-in database to store logs
     * *Defaults to [customLoggingStyle] = [DefaultApplicationLoggingStyle]
     *
     * @param customLoggingStyle To set this property to custom string log format override [LoggingStyle.getFormattedLogData] method
     *
     * */
    fun setLogFormatting(customLoggingStyle: LoggingStyle?) {
        loggingStyle = customLoggingStyle
    }

    /**
     * If set then uses built-in `PinLog Thread.UncaughtExceptionHandler` which
     * is created in a way that it starts a Share Intent mostly starts default EmailApp
     * with logsFile as an attachment. You can provide other info like [toEmails], [message]
     * and [subject] which will appear on the Email Intent extras. If you set [maxBuffer] to `true`
     * number of logcat lines included in the report will be 8000,
     * default is 200 lines which is sufficient for most developers.
     *
     * Following are defaults for Email:
     * - Subject : Application `appName` crashed.
     * - Message : Occurrence : `Date`.
     *
     * You can even provide custom data using [CustomLogFileData.put] methods.
     *
     * If you want this Handler to be disable then use [disablePinLogExceptionHandler].
     *
     * @see CustomLogFileData
     * @see disablePinLogExceptionHandler
     *
     * */
    @JvmStatic
    fun setupPinLogExceptionHandler(
        toEmails: Array<String>?,
        message: String?,
        subject: String?,
        maxBuffer: Boolean = false
    ) {
        toEmails?.let { crashEmailTo = it }
        crashMessage = message
        crashSubject = subject
        crashMaxBuffer = maxBuffer
        logInfo("Exception handling is setTo PinLog")
        setupExceptionHandler()
    }


    /**
     *
     * If you have set the [setupPinLogExceptionHandler] or
     * if you use [getAllPinLogsInFile] function,
     * then this customData will be added to the logsFile.
     *
     * */
    class CustomLogFileData {

        fun put(name: String, value: Boolean): JSONObject {
            return customData.put(name, value)
        }

        fun put(name: String, value: Double): JSONObject {
            return customData.put(name, value)
        }

        fun put(name: String, value: Int): JSONObject {
            return customData.put(name, value)
        }

        fun put(name: String, value: Long): JSONObject {
            return customData.put(name, value)
        }

        fun put(name: String, value: Any?): JSONObject {
            return customData.put(name, value)
        }

        fun get(name: String): Any {
            return customData.get(name)
        }

        fun getBoolean(name: String): Boolean {
            return customData.getBoolean(name)
        }

        fun getDouble(name: String): Double {
            return customData.getDouble(name)
        }

        fun getInt(name: String): Int {
            return customData.getInt(name)
        }

        fun getLong(name: String): Long {
            return customData.getLong(name)
        }

        fun getString(name: String): String {
            return customData.getString(name)
        }

        fun getJSONArray(name: String): JSONArray {
            return customData.getJSONArray(name)
        }

        fun getJSONObject(name: String): JSONObject {
            return customData.getJSONObject(name)
        }

        fun names(): JSONArray? {
            return customData.names()
        }
    }

    /**
     *
     * Disables built-in `PinLog Thread.UncaughtExceptionHandler` if it
     * was previously set using [setupPinLogExceptionHandler]. And sets
     * the handler to previous [Thread.getDefaultUncaughtExceptionHandler].
     *
     * */
    @JvmStatic
    fun disablePinLogExceptionHandler() {
        handleToDefaultExceptionHandler()
    }

    private fun setupExceptionHandler() {
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            PinLogUncaughtExceptionHandler(
                crashEmailTo, crashSubject, crashMessage
            )
        )
    }

    private fun handleToDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
    }


    /**
     * Initialises [PinLog] in Debug mode that is with all Debug Properties like [isDevLoggingEnabled] to true.
     * More suitable **for Debug Builds**.
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class.
     * @param buildConfig The BuildConfig class generated by Gradle when you build the project.
     * @param logingStyle Custom implementation of the [LoggingStyle] class. Default is [DefaultApplicationLoggingStyle]
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    @JvmStatic
    fun initialiseDebug(
        @NonNull application: Application,
        buildConfig: Class<*>? = null,
        logingStyle: LoggingStyle? = null
    ): Boolean {
        isDevLoggingEnabled = true
        shouldStoreLogs = true
        buildConfig?.let {
            buildConfigClass = it
        }
        loggingStyle = if (loggingStyle == null) {
            DefaultApplicationLoggingStyle()
        } else logingStyle

        return instance(application)
    }

    /**
     * Initialises [PinLog] in Release mode that is with all Non-Debug Properties like [setDevLogging] to `false`.
     * This mode is great **for Release Builds** where your app should not
     * print any logs with the [Log] util class.
     *
     * **This means PinLog will not print the Logs to the Logcat, but store them instead
     * which you can get by [PinLog.getAllPinLogs] method**
     *
     * *Note : that the properties will be overwritten by new one if [PinLog] was previously initialised.*
     * @param application Application. Commonly used inside [Application.onCreate] method of your [Application] class.
     * @param buildConfig The BuildConfig class generated by Gradle when you build the project.
     * @param logingStyle Custom implementation of the [LoggingStyle] class. Default is [DefaultApplicationLoggingStyle]
     * @return [Boolean] - If [PinLog] was previously initialised then `false` else `true`.
     *
     * */
    @JvmStatic
    fun initialiseRelease(
        @NonNull application: Application,
        buildConfig: Class<*>? = null,
        logingStyle: LoggingStyle? = null
    ): Boolean {
        isDevLoggingEnabled = false
        shouldStoreLogs = true
        buildConfig?.let {
            buildConfigClass = it
        }
        loggingStyle = if (loggingStyle == null) {
            DefaultApplicationLoggingStyle()
        } else logingStyle
        return instance(application)
    }

    private fun collectBuildConfigData() {
        buildConfigClassData = JSONObject()
        buildConfigClass?.let { someClass ->
            val fields = someClass.fields
            for (field in fields) {
                try {
                    val value: Any? = field[null]

                    value?.let {
                        logInfo(" ${field.name} -> $it ")
                        if (field.type.isArray) {
                            buildConfigClassData!!.put(
                                field.name,
                                JSONArray(listOf(*it as Array<*>))
                            )
                        } else {
                            buildConfigClassData!!.put(field.name, it)
                        }
                    }

                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
        if (buildConfigClassData!!.has("DEBUG")) {
            val isDebug = buildConfigClassData!!.getBoolean("DEBUG")
            if (isDebug) logInfo("Application Build is of Type Debug")
        }
    }

    private class PinLogUncaughtExceptionHandler(
        private val toEmail: Array<String>,
        private val subject: String?,
        private val message: String?
    ) : Thread.UncaughtExceptionHandler {

        override fun uncaughtException(t: Thread, e: Throwable) {
            logWarning("An UncaughtException was caught by PinLog in ${t.name}")
            val stackTrace = getStackTraceString(e)
            logError("$stackTrace \n$e")
            try {
                CrashReporter().sendCrashReportWithEmail(
                    t,
                    e,
                    toEmail,
                    subject,
                    message
                )
            } catch (e: Exception) {
                logWarning("PinLog was unable to handle the exception.")
                defaultExceptionHandler?.uncaughtException(t, e)
            }
        }
    }

    /**
     * This class helps in creating reports and sending that report.
     * @see CrashReporter.createReport
     * @see CrashReporter.sendCrashReportWithEmail
     *
     * */
    class CrashReporter {

        private var crashData: JSONObject = JSONObject()

        /**
         * Creates a report that includes
         * data from [CustomLogFileData], your PinLogs,
         * applicationInfo, BuildConfig if was provided while
         * initialisation.
         *
         * The report returned is a [JSONObject] whose key
         * can be retrieved from [CrashDataKeys].
         *
         * */
        fun createReport(
            t: Thread,
            e: Throwable?,
        ): JSONObject {
            buildCrashData(t, e, customData)
            return crashData
        }

        /**
         * Creates a report that includes
         * data from [CustomLogFileData], your PinLogs,
         * applicationInfo, BuildConfig if was provided while
         * initialisation.
         *
         * The generated Report is added as an attachment
         * to an Email Intent.
         *
         * if the [PinLog.setupPinLogExceptionHandler] method was used by you in Application class,
         * This method is called whenever a Crash occurs.
         *
         * *Note : The Application is force closed after sending Email to
         * avoid any ANRs.*
         *
         * */
        fun sendCrashReportWithEmail(
            t: Thread,
            e: Throwable?,
            toEmail: Array<String>,
            subject: String?,
            message: String?,
        ) {
            buildCrashData(t, e, customData)
            val attachmentUri = createLogsFile()
            val i = createSendIntent(arrayListOf(attachmentUri), toEmail, subject, message)
            i.selector = createResolveIntent()
            logInfo("Granting permissions")
            grantPermission(i, attachmentUri)
            createPendingIntentAndLaunch(arrayOf(i))
        }

        private fun createSendIntent(
            attachments: ArrayList<Uri>,
            toEmail: Array<String>? = arrayOf(),
            subject: String? = null,
            message: String? = null
        ): Intent {
            return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject ?: "Application \"$callerAppName\" crashed.")
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments)
                putExtra(Intent.EXTRA_EMAIL, toEmail)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Occurrence : " + getDate() + if (message != null) "\n$message" else ""
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
        }

        private fun createResolveIntent(): Intent {
            val i = Intent(Intent.ACTION_SENDTO)
            i.data = Uri.parse("mailto:")
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return i
        }

        private fun createPendingIntentAndLaunch(
            intents: Array<Intent>
        ) {
            logInfo("Starting intent")
            getContext().startActivity(intents.first())
            this.crashData = JSONObject()
            logInfo("Ending application")
            exitProcess(-2)
        }

        private fun grantPermission(i: Intent, attachmentUri: Uri) {
            try {
                for (resolveInfo in getContext().packageManager.queryIntentActivities(
                    i,
                    PackageManager.MATCH_DEFAULT_ONLY
                )) {
                    getContext().grantUriPermission(
                        resolveInfo.resolvePackageName,
                        attachmentUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    logInfo("Granted permission to : ${resolveInfo.resolvePackageName}")
                }
            } catch (e: Exception) {
            }
        }

        companion object CrashDataKeys {
            const val APP_NAME = "APP_NAME"
            const val INSTALLATION_ID = "INSTALLATION_ID"
            const val PACKAGE_NAME = "PACKAGE_NAME"
            const val CRASH_DATE = "CRASH_DATE"
            const val DATA_DIR = "DATA_DIR"
            const val LOG_FILES = "LOG_FILES"
            const val THREAD_NAME = "THREAD_NAME"
            const val STACKTRACE = "STACKTRACE"
            const val CAUSE = "CAUSE"
            const val MESSAGE = "MESSAGE"
            const val LOGS = "LOGS"
            const val MODEL = "MODEL"
            const val MANUFACTURER = "MANUFACTURER"
            const val DEVICE = "DEVICE"
            const val TYPE = "TYPE"
            const val RELEASE = "RELEASE"
            const val CODENAME = "CODENAME"
            const val SDK_INT = "SDK_INT"
            const val DEBUG = "DEBUG"
            const val VERSION_CODE = "VERSION_CODE"
            const val VERSION_NAME = "VERSION_NAME"
            const val PREFS = "PREFS"
            const val ORIENTATION = "ORIENTATION"
            const val CUSTOM_DATA = "CUSTOM_DATA"
        }

        private fun buildCrashData(
            t: Thread, e: Throwable?, customLogsFileData: JSONObject = JSONObject()
        ) {
            crashData = JSONObject().apply {
                put("APP_NAME", callerAppName)
                put("INSTALLATION_ID", Installation.id(getContext()))
                put("PACKAGE_NAME", callerPackageName)
                put("CRASH_DATE", getDate())
                put("CUSTOM_DATA", customLogsFileData)
                put("DATA_DIR", Environment.getDataDirectory())
                put("LOG_FILES", getLogsFilesNames() ?: "No Files")
                put("THREAD_NAME", t.name)
                put("STACKTRACE", getStackTraceString(e))
                put("CAUSE", e?.cause ?: "No cause")
                put("MESSAGE", e?.message ?: "No message")
                put("LOGS", getAllPinLogsAsString() ?: "None")
                put("MODEL", Build.MODEL)
                put("MANUFACTURER", Build.MANUFACTURER)
                put("DEVICE", Build.DEVICE)
                put("TYPE", Build.TYPE)
                put("RELEASE", Build.VERSION.RELEASE)
                put("CODENAME", Build.VERSION.CODENAME)
                put("SDK_INT", Build.VERSION.SDK_INT)
                put(
                    "DEBUG",
                    (0 != getContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
                )
                put("VERSION_CODE", callerVersionCode)
                put("VERSION_NAME", callerVersionName)
                put("PREFS", getPrefsInfo())
            }
            val orientation = when (getContext().resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> "ORIENTATION_LANDSCAPE"
                Configuration.ORIENTATION_PORTRAIT -> "ORIENTATION_PORTRAIT"
                else -> "ORIENTATION_UNDEFINED"
            }
            crashData.put("ORIENTATION", orientation)
            buildConfigClassData?.let { crashData.put("BUILD_CONFIG", it) }
            getLogcat()
        }

        private fun getLogsFilesNames(): String? {
            var names = ""
            getLogFilesDir().listFiles()?.forEach { names += it.name + "\n |" }
            return if (names.trim().isEmpty()) null
            else names
        }

        private fun getPrefsInfo(): JSONObject {
            val entries = getDefaultSharedPreferences().all
            return JSONObject().put("DEFAULT_SHARED_PREFS", JSONObject(entries))
        }

        private fun getDefaultSharedPreferences(): SharedPreferences {
            return getContext()
                .getSharedPreferences(callerPackageName + "_preferences", Context.MODE_PRIVATE)
        }

        private fun getLogcat() {
            try {
                val p = Runtime.getRuntime().exec("logcat -d")
                val logcat =
                    if (!crashMaxBuffer) {
                        BufferedReader(
                            InputStreamReader(p.inputStream),
                            DEFAULT_LOG_BUFFER_SIZE
                        )
                    } else {
                        BufferedReader(InputStreamReader(p.inputStream))
                    }
                val str = logcat.readText()
                logcat.close()
                crashData.put("*******APPLICATION_LOGCAT_LINES*******", str)
            } catch (e: Exception) {
                logWarning("Could'nt read logcat : $e")
            }
        }

        private fun createLogsFile(): Uri {
            logWarning("Saving logs in a file.")
            val logsFolder = getLogFilesDir()
            if (!logsFolder.exists()) logsFolder.mkdir()
            val logsFile = File(
                logsFolder, getFileName()
            )
            logsFile.createNewFile()
            return writeLogsInFile(logsFile)
        }

        @SuppressLint("SetWorldReadable")
        private fun writeLogsInFile(logsFile: File): Uri {
            logsFile.writeText(crashData.toString())
            logsFile.setReadable(true, false)
            logInfo("logs were written to file : ${logsFile.name}")
            return try {
                FileProvider.getUriForFile(getContext(), getAuthority(), logsFile)
            } catch (e: Exception) {
                Uri.fromFile(logsFile)
            }
        }

    }

    private fun getDate(format: String = DATE_FORMAT): String {
        return SimpleDateFormat(format, Locale.ENGLISH).format(Date())
    }

    private fun getAuthority(): String {
        return callerPackageName + PROVIDER_SUFFIX
    }

    private fun getFileName(): String {
        var name = ""
        val n = callerAppName.split(" ")
        n.forEach {
            name += it.toUpperCase(Locale.ROOT) + "_"
        }
        return name + "_" + getDate(FILE_DATE_FORMAT) + "_LOG.txt"
    }

    /**
     * Returns file where all log files will be/are stored.
     * You can use this to find if user has your app's log files on device.
     * */
    fun getLogFilesDir(): File {
        return File(getContext().filesDir, LOGS_DIR_NAME)
    }

    /**
     *
     * **Warning : Do not use this value as your TAG property as logs
     * having this [CLASS_TAG] are ignored.**
     *
     * */
    internal const val CLASS_TAG = "PinLog"

    /**
     * Same as one defined in [R.xml.provider_paths]
     * */
    private const val LOGS_DIR_NAME = "logs_app_dir"
    private const val DATE_FORMAT = "dd.MM.yyyy h:mm:ss a z"
    private const val FILE_DATE_FORMAT = "dd_MM_yyyy_h_mm_a"
    private const val PROVIDER_SUFFIX = ".pinlog.provider"
    private const val DEFAULT_LOG_BUFFER_SIZE = 207
    private const val LONG_LOG_BUFFER_SIZE = 8192

}

