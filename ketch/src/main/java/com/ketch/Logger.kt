package com.ketch

interface Logger {
    companion object {
        const val TAG = "KetchLogs"
    }

    fun log(
        tag: String? = TAG,
        msg: String? = "",
        tr: Throwable? = null,
        type: LogType = LogType.DEBUG
    )
}

enum class LogType {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}
