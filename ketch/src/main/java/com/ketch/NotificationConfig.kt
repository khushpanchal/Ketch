package com.ketch

import com.ketch.internal.utils.NotificationConst
import kotlinx.serialization.Serializable

@Serializable
data class NotificationConfig(
    val contentText: NotificationContentText = NotificationContentText(),
    val enabled: Boolean = NotificationConst.DEFAULT_VALUE_NOTIFICATION_ENABLED,
    val channelName: String = NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_NAME,
    val channelDescription: String = NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_DESCRIPTION,
    val importance: Int = NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_IMPORTANCE,
    val showSpeed: Boolean = true,
    val showSize: Boolean = true,
    val showTime: Boolean = true,
    val smallIcon: Int
)

@Serializable
data class NotificationContentText(
    val success: String = NotificationConst.DEFAULT_VALUE_NOTIFICATION_COMPLETED,
    val failed: String = NotificationConst.DEFAULT_VALUE_NOTIFICATION_FAILED,
    val pause: String = NotificationConst.DEFAULT_VALUE_NOTIFICATION_PAUSED,
    val cancelled: String = NotificationConst.DEFAULT_VALUE_NOTIFICATION_CANCELLED
)