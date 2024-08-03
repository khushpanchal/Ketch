package com.ketch.internal.utils

import android.util.Log
import com.ketch.LogType
import com.ketch.Logger

internal class DownloadLogger(private val enableLogs: Boolean) : Logger {
    override fun log(tag: String?, msg: String?, tr: Throwable?, type: LogType) {
        if (enableLogs) {
            when (type) {
                LogType.VERBOSE -> Log.v(tag, msg, tr)
                LogType.DEBUG -> Log.d(tag, msg, tr)
                LogType.INFO -> Log.i(tag, msg, tr)
                LogType.WARN -> Log.w(tag, msg, tr)
                LogType.ERROR -> Log.e(tag, msg, tr)
            }
        }
    }
}
