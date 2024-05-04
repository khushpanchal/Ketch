package com.ketch.internal.utils

internal object NotificationHelper {

    private val dismissedNotificationIds = mutableListOf<Int>()

    fun addToDismissedNotificationIds(id: Int) {
        dismissedNotificationIds.add(id)
    }

    fun isDismissedNotification(id: Int?): Boolean {
        return id in dismissedNotificationIds
    }
}