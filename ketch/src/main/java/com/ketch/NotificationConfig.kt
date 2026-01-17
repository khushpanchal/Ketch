package com.ketch

import com.ketch.internal.utils.NotificationConst
import kotlinx.serialization.Serializable

@Serializable
data class NotificationConfig(
    val enabled: Boolean = NotificationConst.DEFAULT_VALUE_NOTIFICATION_ENABLED,
    val channelName: String = NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_NAME,
    val channelDescription: String = NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_DESCRIPTION,
    val importance: Int = NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_IMPORTANCE,
    val showSpeed: Boolean = true,
    val showSize: Boolean = true,
    val showTime: Boolean = true,
    val smallIcon: Int,
    val smallIcons: NotificationSmallIconConfig = NotificationSmallIconConfig(
        progress = smallIcon,
        success = smallIcon,
        failed = smallIcon,
        paused = smallIcon,
        cancelled = smallIcon
    )
)

@Serializable
data class NotificationSmallIconConfig(
    val progress: Int,
    val success: Int,
    val failed: Int,
    val paused: Int,
    val cancelled: Int
)