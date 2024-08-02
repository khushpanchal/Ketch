package com.ketch.internal.utils

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.ketch.DownloadConfig
import com.ketch.NotificationConfig
import com.ketch.internal.download.DownloadRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal object WorkUtil {
    private val gson = Gson()

    fun DownloadRequest.toJson(): String {
        return gson.toJson(this)
    }

    fun jsonToDownloadRequest(jsonStr: String): DownloadRequest {
        return gson.fromJson(jsonStr, DownloadRequest::class.java)
    }

    fun DownloadConfig.toJson(): String {
        return gson.toJson(this)
    }

    fun jsonToDownloadConfig(jsonStr: String): DownloadConfig {
        if (jsonStr.isEmpty()) return DownloadConfig()
        return gson.fromJson(jsonStr, DownloadConfig::class.java)
    }

    fun NotificationConfig.toJson(): String {
        return gson.toJson(this)
    }

    fun jsonToNotificationConfig(jsonStr: String): NotificationConfig {
        if (jsonStr.isEmpty()) {
            return NotificationConfig(smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON)
        }
        return gson.fromJson(jsonStr, NotificationConfig::class.java)
    }

    fun hashMapToJson(headers: HashMap<String, String>): String {
        if (headers.isEmpty()) return ""
        return gson.toJson(headers)
    }

    fun jsonToHashMap(jsonString: String): HashMap<String, String> {
        if (jsonString.isEmpty()) return hashMapOf()
        val type = object : TypeToken<HashMap<String, String>>() {}.type
        return gson.fromJson(jsonString, type)
    }

    fun removeNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
