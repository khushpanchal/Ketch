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

internal class DownloadNotificationManager(
    private val context: Context,
    private val notificationConfig: NotificationConfig,
    private val requestId: Int,
    private val fileName: String
) {

    private var foregroundInfo: ForegroundInfo? = null
    private val notificationBuilder =
        NotificationCompat.Builder(context, NotificationConst.NOTIFICATION_CHANNEL_ID)
    private val notificationId = requestId

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    fun sendUpdateNotification(
        progress: Int = 0,
        speedInBPerMs: Float = 0F,
        length: Long = 0L,
        update: Boolean = false
    ): ForegroundInfo? {
        if (update) {
            foregroundInfo = ForegroundInfo(
                notificationId,
                notificationBuilder
                    .setProgress(DownloadConst.MAX_VALUE_PROGRESS, progress, false)
                    .setContentText(
                        setContentTextNotification(speedInBPerMs, progress, length)
                    )
                    .setSubText("$progress%")
                    .build(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
            )
        } else {
            // Open Application
            val intentOpen = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intentOpen?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

            foregroundInfo = ForegroundInfo(
                notificationId,
                notificationBuilder
                    .setSmallIcon(notificationConfig.smallIcon)
                    .setContentTitle("Downloading $fileName")
                    .setContentIntent(pendingIntentOpen)
                    .setProgress(DownloadConst.MAX_VALUE_PROGRESS, progress, false)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .addAction(-1, NotificationConst.PAUSE_BUTTON_TEXT, pendingIntentPause)
                    .addAction(-1, NotificationConst.CANCEL_BUTTON_TEXT, pendingIntentCancel)
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

    private fun setContentTextNotification(
        speedInBPerMs: Float,
        progress: Int,
        length: Long
    ): String {
        var text = ""
        val timeLeftText = TextUtil.getTimeLeftText(speedInBPerMs, progress, length)
        val speedText = TextUtil.getSpeedText(speedInBPerMs)
        val lengthText = TextUtil.getTotalLengthText(length)
        if (notificationConfig.showTime) text += timeLeftText

        if ((notificationConfig.showTime && timeLeftText.isNotEmpty())
            && (notificationConfig.showSpeed || notificationConfig.showSize)
        ) text += ", "

        if (notificationConfig.showSpeed) text += speedText

        if (notificationConfig.showSpeed && notificationConfig.showSize) text += ", "

        if (notificationConfig.showSize) text += "total: $lengthText"
        return text
    }

    fun sendDownloadSuccessNotification(totalLength: Long) {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME,
                    notificationConfig.smallIcon
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

    fun sendDownloadFailedNotification() {
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
                action = NotificationConst.ACTION_DOWNLOAD_FAILED
            }
        )
    }

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

    fun sendDownloadPausedNotification() {
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
                action = NotificationConst.ACTION_DOWNLOAD_PAUSED
            }
        )
    }

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
