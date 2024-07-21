package com.ketch.internal.utils

internal object NotificationConst {
    const val NOTIFICATION_CHANNEL_ID = "download_channel"
    const val DEFAULT_VALUE_NOTIFICATION_ENABLED = false
    const val KEY_NOTIFICATION_CHANNEL_NAME = "key_notification_channel_name"
    const val DEFAULT_VALUE_NOTIFICATION_CHANNEL_NAME = "File Download"
    const val KEY_NOTIFICATION_CHANNEL_DESCRIPTION = "key_notification_channel_description"
    const val DEFAULT_VALUE_NOTIFICATION_CHANNEL_DESCRIPTION = "Notify file download status"
    const val KEY_NOTIFICATION_CHANNEL_IMPORTANCE = "key_notification_channel_importance"
    const val DEFAULT_VALUE_NOTIFICATION_CHANNEL_IMPORTANCE = 2 // LOW
    const val KEY_NOTIFICATION_SMALL_ICON = "key_small_notification_icon"
    const val DEFAULT_VALUE_NOTIFICATION_SMALL_ICON = -1
    const val KEY_NOTIFICATION_ID = "key_notification_id"

    // Actions
    const val ACTION_NOTIFICATION_DISMISSED = "ACTION_NOTIFICATION_DISMISSED"
    const val ACTION_DOWNLOAD_COMPLETED = "ACTION_DOWNLOAD_COMPLETED"
    const val ACTION_DOWNLOAD_FAILED = "ACTION_DOWNLOAD_FAILED"
    const val ACTION_DOWNLOAD_CANCELLED = "ACTION_DOWNLOAD_CANCELLED"
    const val ACTION_DOWNLOAD_PAUSED = "ACTION_NOTIFICATION_PAUSED"
    const val ACTION_NOTIFICATION_RESUME_CLICK = "ACTION_NOTIFICATION_RESUME_CLICK"
    const val ACTION_NOTIFICATION_RETRY_CLICK = "ACTION_NOTIFICATION_RETRY_CLICK"
    const val ACTION_NOTIFICATION_PAUSE_CLICK = "ACTION_NOTIFICATION_PAUSE_CLICK"
    const val ACTION_NOTIFICATION_CANCEL_CLICK = "ACTION_NOTIFICATION_CANCEL_CLICK"

    // Cancel
    const val CANCEL_BUTTON_TEXT = "Cancel"
    const val PAUSE_BUTTON_TEXT = "Pause"
    const val RESUME_BUTTON_TEXT = "Resume"
    const val RETRY_BUTTON_TEXT = "Retry"
}
