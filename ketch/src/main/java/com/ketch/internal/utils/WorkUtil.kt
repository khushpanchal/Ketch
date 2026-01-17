package com.ketch.internal.utils

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.ketch.ButtonTextConfig
import com.ketch.NotificationConfig
import com.ketch.internal.download.DownloadContentTexts
import com.ketch.internal.download.DownloadRequest
import com.ketch.internal.download.DownloadTitles
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object WorkUtil {

    fun DownloadRequest.toJson(): String {
        return Json.encodeToString(this)
    }

    fun jsonToDownloadRequest(jsonStr: String): DownloadRequest {
        return Json.decodeFromString(jsonStr)
    }

    fun NotificationConfig.toJson(): String {
        return Json.encodeToString(this)
    }

    fun ButtonTextConfig.toJson(): String {
        return Json.encodeToString(this)
    }

    fun jsonToNotificationConfig(jsonStr: String): NotificationConfig {
        if (jsonStr.isEmpty()) {
            return NotificationConfig(smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON)
        }
        return Json.decodeFromString(jsonStr)
    }

    fun jsonToButtonTextConfig(jsonStr: String): ButtonTextConfig {
        if (jsonStr.isEmpty()) {
            return ButtonTextConfig()
        }
        return Json.decodeFromString(jsonStr)
    }

    fun jsonToDownloadTitles(jsonStr: String): DownloadTitles {
        if (jsonStr.isEmpty()) {
            return DownloadTitles()
        }
        return Json.decodeFromString(jsonStr)
    }

    fun DownloadTitles.toJson(): String {
        return Json.encodeToString(this)
    }

    fun jsonToDownloadContentTexts(jsonStr: String): DownloadContentTexts {
        if (jsonStr.isEmpty()) {
            return DownloadContentTexts()
        }
        return Json.decodeFromString(jsonStr)
    }

    fun DownloadContentTexts.toJson(): String {
        return Json.encodeToString(this)
    }

    fun hashMapToJson(headers: HashMap<String, String>): String {
        if (headers.isEmpty()) return ""
        return Json.encodeToString(headers)
    }

    fun jsonToHashMap(jsonString: String): HashMap<String, String> {
        if (jsonString.isEmpty()) return hashMapOf()
        return Json.decodeFromString(jsonString)
    }

    fun removeNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
