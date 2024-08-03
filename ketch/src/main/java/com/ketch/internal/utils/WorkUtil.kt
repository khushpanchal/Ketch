package com.ketch.internal.utils

import com.ketch.DownloadConfig
import com.ketch.NotificationConfig
import com.ketch.internal.download.DownloadRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object WorkUtil {

    fun DownloadRequest.toJson(): String {
        return Json.encodeToString(this)
    }

    fun jsonToDownloadRequest(jsonStr: String): DownloadRequest {
        return Json.decodeFromString(jsonStr)
    }

    fun DownloadConfig.toJson(): String {
        return Json.encodeToString(this)
    }

    fun jsonToDownloadConfig(jsonStr: String): DownloadConfig {
        if (jsonStr.isEmpty()) return DownloadConfig()
        return Json.decodeFromString(jsonStr)
    }

    fun NotificationConfig.toJson(): String {
        return Json.encodeToString(this)
    }

    fun jsonToNotificationConfig(jsonStr: String): NotificationConfig {
        if (jsonStr.isEmpty()) return NotificationConfig(smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON)
        return Json.decodeFromString(jsonStr)
    }

    fun hashMapToJson(headers: HashMap<String, String>): String {
        if (headers.isEmpty()) return ""
        return Json.encodeToString(headers)
    }

    fun jsonToHashMap(jsonString: String): HashMap<String, String> {
        if (jsonString.isEmpty()) return hashMapOf()
        return Json.decodeFromString(jsonString)
    }
}
