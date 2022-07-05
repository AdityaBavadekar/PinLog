package com.adityaamolbavadekar.pinlog.installation

import android.content.Context
import com.adityaamolbavadekar.pinlog.PinLog
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Copied from developer.android.com/blogs
 * */
internal object Installation {

    private const val INSTALLATION = "PINLOG-INSTALLATION"

    @JvmStatic
    @Synchronized
    fun id(context: Context): String {
        val installation = File(context.filesDir, INSTALLATION)
        return try {
            if (!installation.exists()) {
                installation.createNewFile()
                installation.writeText(UUID.randomUUID().toString())
                PinLog.logInfo("Installation Identifier generated and saved.")
            }
            installation.readText()
        } catch (e: IOException) {
            PinLog.logWarning("Couldn't retrieve the Installation Identifier : $e")
            "Couldn't retrieve the Installation Identifier"
        } catch (e: RuntimeException) {
            PinLog.logWarning("Couldn't retrieve the Installation Identifier : $e")
            "Couldn't retrieve the Installation Identifier"
        }
    }

}