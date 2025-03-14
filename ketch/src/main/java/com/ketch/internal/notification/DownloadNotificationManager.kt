package com.ketch.internal.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.ketch.NotificationConfig
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.NotificationConst
import com.ketch.internal.utils.TextUtil
import com.ketch.internal.utils.WorkUtil.removeNotification

/**
 * Download notification manager: Responsible for showing the in progress notification for each downloads.
 * Whenever the download is cancelled or paused or failed (terminating state), WorkManager cancels the
 * ongoing notification and this class sends the broadcast to show terminating state notifications.
 *
 * Notification ID = Download request ID for each download.
 *
 * @property context Application context
 * @property notificationConfig [NotificationConfig]
 * @property requestId Unique ID for current download
 * @property fileName File name of the download
 * @constructor Create empty Download notification manager
 */
internal class DownloadNotificationManager(
    private val context: Context,
    private val notificationConfig: NotificationConfig,
    private val requestId: Int,
    private val fileName: String
) {

    private var foregroundInfo: ForegroundInfo? = null
    private val notificationBuilder =
        NotificationCompat.Builder(context, NotificationConst.NOTIFICATION_CHANNEL_ID)
    private val notificationId = requestId // notification id is same as request id

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    /**
     * Send update notification: Shows the current in progress download notification, which cannot be dismissed
     *
     * @param progress current download progress
     * @param speedInBPerMs current download speed in byte per second
     * @param length current length of download file in bytes
     * @param update Boolean to check if showing notification for first time of updating it
     * @return ForegroundInfo to be set in Worker
     */
    fun sendUpdateNotification(
        progress: Int = 0,
        speedInBPerMs: Float = 0F,
        length: Long = 0L,
        update: Boolean = false
    ): ForegroundInfo? {
        if (update) {

            var nb = notificationBuilder
                .setProgress(DownloadConst.MAX_VALUE_PROGRESS, progress, if (length == 0L) true else false)

            if (length != 0L) {
                nb = nb.setContentText(
                    setContentTextNotification(speedInBPerMs, progress, length)
                ).setSubText("$progress%")
            }

            foregroundInfo = ForegroundInfo(
                notificationId,
                nb.build(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
            )
        } else {
            // Remove any previous notification
            removeNotification(context, requestId) // In progress notification
            removeNotification(context, requestId + 1) // Cancelled, Paused, Failed, Success notification

            // Open Application (Send the unique download request id in intent)
            val intentOpen = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intentOpen?.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intentOpen?.putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
            val pendingIntentOpen =
                PendingIntent.getActivity(
                    context.applicationContext,
                    notificationId,
                    intentOpen,
                    PendingIntent.FLAG_IMMUTABLE
                )

            // Dismiss Notification
            val intentDismiss = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationConst.ACTION_NOTIFICATION_DISMISSED
            }
            intentDismiss.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
            val pendingIntentDismiss = PendingIntent.getBroadcast(
                context.applicationContext,
                notificationId,
                intentDismiss,
                PendingIntent.FLAG_IMMUTABLE
            )

            // Pause Notification
            val intentPause = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationConst.ACTION_NOTIFICATION_PAUSE_CLICK
            }
            intentPause.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
            intentPause.putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
            val pendingIntentPause = PendingIntent.getBroadcast(
                context.applicationContext,
                notificationId,
                intentPause,
                PendingIntent.FLAG_IMMUTABLE
            )

            // Cancel Notification
            val intentCancel = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationConst.ACTION_NOTIFICATION_CANCEL_CLICK
            }
            intentCancel.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
            intentCancel.putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
            val pendingIntentCancel = PendingIntent.getBroadcast(
                context.applicationContext,
                notificationId,
                intentCancel,
                PendingIntent.FLAG_IMMUTABLE
            )

            var nb = notificationBuilder
                .setSmallIcon(notificationConfig.smallIcon)
                .setContentTitle("Downloading $fileName")
                .setContentIntent(pendingIntentOpen)
                .setProgress(DownloadConst.MAX_VALUE_PROGRESS, progress, if (length == 0L) true else false)
                .setOnlyAlertOnce(true)
                .setOngoing(true)

            if (length != 0L) {
                nb = nb.addAction(-1, NotificationConst.PAUSE_BUTTON_TEXT, pendingIntentPause)
            }

            foregroundInfo = ForegroundInfo(
                notificationId,
                nb.addAction(-1, NotificationConst.CANCEL_BUTTON_TEXT, pendingIntentCancel)
                    .setDeleteIntent(pendingIntentDismiss)
                    .build(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
            )
        }
        return foregroundInfo
    }

    /**
     * Set content text notification
     *
     * @param speedInBPerMs speed in byte per second of download
     * @param progress current progress of download
     * @param length total size of progress
     * @return Return the text to be displayed on in-progress download notification
     */
    private fun setContentTextNotification(
        speedInBPerMs: Float,
        progress: Int,
        length: Long
    ): String {
        val timeLeftText = TextUtil.getTimeLeftText(speedInBPerMs, progress, length)
        val speedText = TextUtil.getSpeedText(speedInBPerMs)
        val lengthText = TextUtil.getTotalLengthText(length)

        val parts = mutableListOf<String>()

        if (notificationConfig.showTime && timeLeftText.isNotEmpty()) {
            parts.add(timeLeftText)
        }

        if (notificationConfig.showSpeed && speedText.isNotEmpty()) {
            parts.add(speedText)
        }

        if (notificationConfig.showSize && lengthText.isNotEmpty()) {
            parts.add("total: $lengthText")
        }

        return parts.joinToString(", ")
    }

    /**
     * Send broadcast to show download success notification
     *
     * @param totalLength
     */
    fun sendDownloadSuccessNotification(totalLength: Long) {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME,
                    notificationConfig.channelName
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_IMPORTANCE,
                    notificationConfig.importance
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_DESCRIPTION,
                    notificationConfig.channelDescription
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_SMALL_ICON,
                    notificationConfig.smallIcon
                )
                putExtra(DownloadConst.KEY_FILE_NAME, fileName)
                putExtra(DownloadConst.KEY_LENGTH, totalLength)
                putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
                putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
                action = NotificationConst.ACTION_DOWNLOAD_COMPLETED
            }
        )
    }

    /**
     * Send broadcast to show download failed notification
     *
     * @param currentProgress current download progress
     */
    fun sendDownloadFailedNotification(currentProgress: Int) {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME,
                    notificationConfig.channelName
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_IMPORTANCE,
                    notificationConfig.importance
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_DESCRIPTION,
                    notificationConfig.channelDescription
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_SMALL_ICON,
                    notificationConfig.smallIcon
                )
                putExtra(DownloadConst.KEY_FILE_NAME, fileName)
                putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
                putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
                putExtra(DownloadConst.KEY_PROGRESS, currentProgress)
                action = NotificationConst.ACTION_DOWNLOAD_FAILED
            }
        )
    }

    /**
     * Send broadcast to show download cancelled notification
     *
     */
    fun sendDownloadCancelledNotification() {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME,
                    notificationConfig.channelName
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_IMPORTANCE,
                    notificationConfig.importance
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_DESCRIPTION,
                    notificationConfig.channelDescription
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_SMALL_ICON,
                    notificationConfig.smallIcon
                )
                putExtra(DownloadConst.KEY_FILE_NAME, fileName)
                putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
                putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
                action = NotificationConst.ACTION_DOWNLOAD_CANCELLED
            }
        )
    }

    /**
     * Send broadcast to show download paused notification
     *
     * @param currentProgress current download progress
     */
    fun sendDownloadPausedNotification(currentProgress: Int) {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME,
                    notificationConfig.channelName
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_IMPORTANCE,
                    notificationConfig.importance
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_DESCRIPTION,
                    notificationConfig.channelDescription
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_SMALL_ICON,
                    notificationConfig.smallIcon
                )
                putExtra(DownloadConst.KEY_FILE_NAME, fileName)
                putExtra(DownloadConst.KEY_PROGRESS, currentProgress)
                putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
                putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
                action = NotificationConst.ACTION_DOWNLOAD_PAUSED
            }
        )
    }

    /**
     * Create notification channel for File downloads
     *
     */
    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NotificationConst.NOTIFICATION_CHANNEL_ID,
            notificationConfig.channelName,
            notificationConfig.importance
        )
        channel.description = notificationConfig.channelDescription
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

}
