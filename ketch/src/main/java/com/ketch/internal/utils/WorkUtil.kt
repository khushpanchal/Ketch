package com.ketch.internal.utils

import com.ketch.DownloadConfig
import com.ketch.NotificationConfig
import com.ketch.internal.download.DownloadRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal object WorkUtil {

    fun DownloadRequest.toJson(): String {
        return Gson().toJson(this)
    }

    fun jsonToDownloadRequest(jsonStr: String): DownloadRequest {
        return Gson().fromJson(jsonStr, DownloadRequest::class.java)
    }

    fun DownloadConfig.toJson(): String {
        return Gson().toJson(this)
    }

    fun jsonToDownloadConfig(jsonStr: String): DownloadConfig {
        if (jsonStr.isEmpty()) return DownloadConfig()
        return Gson().fromJson(jsonStr, DownloadConfig::class.java)
    }

    fun NotificationConfig.toJson(): String {
        return Gson().toJson(this)
    }

    fun jsonToNotificationConfig(jsonStr: String): NotificationConfig {
        if (jsonStr.isEmpty()) return NotificationConfig(smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON)
        return Gson().fromJson(jsonStr, NotificationConfig::class.java)
    }

    fun hashMapToJson(headers: HashMap<String, String>): String {
        if (headers.isEmpty()) return ""
        val gson = Gson()
        return gson.toJson(headers)
    }

    fun jsonToHashMap(jsonString: String): HashMap<String, String> {
        if (jsonString.isEmpty()) return hashMapOf()
        val gson = Gson()
        val type = object : TypeToken<HashMap<String, String>>() {}.type
        return gson.fromJson(jsonString, type)
    }
}
