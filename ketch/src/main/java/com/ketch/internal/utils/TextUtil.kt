package com.ketch.internal.utils

internal object TextUtil {

    private const val MAX_PERCENT = 100
    private const val VALUE_60 = 60
    private const val VALUE_3 = 3
    private const val VALUE_300 = 300
    private const val VALUE_500 = 500
    private const val VALUE_1024 = 1024
    private const val SEC_IN_MILLIS = 1000

    fun getTimeLeftText(speedInBPerMs: Float, progressPercent: Int, lengthInBytes: Long): String {
        if (speedInBPerMs == 0F) return ""
        val speedInBPerSecond = speedInBPerMs * SEC_IN_MILLIS
        val bytesLeft = (lengthInBytes * (MAX_PERCENT - progressPercent) / MAX_PERCENT).toFloat()

        val secondsLeft = bytesLeft / speedInBPerSecond
        val minutesLeft = secondsLeft / VALUE_60
        val hoursLeft = minutesLeft / VALUE_60

        return when {
            secondsLeft < VALUE_60 -> "%.0f s left".format(secondsLeft)
            minutesLeft < VALUE_3 -> "%.0f mins %.0f s left".format(minutesLeft, secondsLeft % VALUE_60)
            minutesLeft < VALUE_60 -> "%.0f mins left".format(minutesLeft)
            minutesLeft < VALUE_300 -> "%.0f hrs %.0f mins left".format(hoursLeft, minutesLeft % VALUE_60)
            else -> "%.0f hrs left".format(hoursLeft)
        }
    }

    fun getSpeedText(speedInBPerMs: Float): String {
        var value = speedInBPerMs * SEC_IN_MILLIS
        val units = arrayOf("b/s", "kb/s", "mb/s", "gb/s")
        var unitIndex = 0

        while (value >= VALUE_500 && unitIndex < units.size - 1) {
            value /= VALUE_1024
            unitIndex++
        }

        return "%.2f %s".format(value, units[unitIndex])
    }

    fun getTotalLengthText(lengthInBytes: Long): String {
        var value = lengthInBytes.toFloat()
        val units = arrayOf("b", "kb", "mb", "gb")
        var unitIndex = 0

        while (value >= VALUE_500 && unitIndex < units.size - 1) {
            value /= VALUE_1024
            unitIndex++
        }

        return "%.2f %s".format(value, units[unitIndex])
    }

}
