package com.adityaamolbavadekar.pinlogactivity

import com.adityaamolbavadekar.pinlog.PinLog

val Int.toText: String
    get() {
        return when (this) {
            PinLog.LogLevel.INFO.LEVEL_INT -> "INFO"
            PinLog.LogLevel.WARN.LEVEL_INT -> "WARN"
            PinLog.LogLevel.ERROR.LEVEL_INT -> "ERROR"
            PinLog.LogLevel.DEBUG.LEVEL_INT -> "DEBUG"
            else -> "Unknown"
        }
    }