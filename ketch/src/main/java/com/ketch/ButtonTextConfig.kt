package com.ketch

import com.ketch.internal.utils.NotificationConst
import kotlinx.serialization.Serializable

@Serializable
data class ButtonTextConfig(
    val cancel: String = NotificationConst.CANCEL_BUTTON_TEXT,
    val pause: String = NotificationConst.PAUSE_BUTTON_TEXT,
    val resume: String = NotificationConst.RESUME_BUTTON_TEXT,
    val retry: String = NotificationConst.RETRY_BUTTON_TEXT
)