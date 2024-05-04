package com.ketch.internal.notification

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
import androidx.work.WorkManager
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.NotificationConst
import com.ketch.internal.utils.TextUtil
import java.util.UUID

internal class DownloadNotificationManager(
    private val context: Context,
    private val notificationChannelName: String,
    private val notificationChannelDescription: String,
    private val notificationImportance: Int,
    private val requestId: Int,
    private val notificationSmallIcon: Int,
    private val fileName: String,
    private val workId: UUID
) {

    private var foregroundInfo: ForegroundInfo? = null
    private val notificationBuilder =
        NotificationCompat.Builder(context, NotificationConst.NOTIFICATION_CHANNEL_ID)
    private val notificationId = ((requestId + System.currentTimeMillis()) % Int.MAX_VALUE).toInt()

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
                        "${
                            TextUtil.getTimeLeftText(
                                speedInBPerMs,
                                progress,
                                length
                            )
                        }, ${TextUtil.getSpeedText(speedInBPerMs)}, total: ${
                            TextUtil.getTotalLengthText(
                                length
                            )
                        }"
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
            //Open Application
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

            //Dismiss Notification
            val intentDismiss = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationConst.ACTION_NOTIFICATION_DISMISSED
            }
            intentDismiss.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
            val pendingIntentDismiss = PendingIntent.getBroadcast(
                context.applicationContext, notificationId, intentDismiss,
                PendingIntent.FLAG_IMMUTABLE
            )

            //Cancel Download
            val pendingIntentCancel = WorkManager.getInstance(context.applicationContext)
                .createCancelPendingIntent(workId)

            foregroundInfo = ForegroundInfo(
                notificationId,
                notificationBuilder
                    .setSmallIcon(notificationSmallIcon)
                    .setContentTitle("Downloading $fileName")
                    .setContentIntent(pendingIntentOpen)
                    .setProgress(DownloadConst.MAX_VALUE_PROGRESS, progress, false)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
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

    fun sendDownloadSuccessNotification(totalLength: Long) {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtra(NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME, notificationChannelName)
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_IMPORTANCE,
                    notificationImportance
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_DESCRIPTION,
                    notificationChannelDescription
                )
                putExtra(NotificationConst.KEY_NOTIFICATION_SMALL_ICON, notificationSmallIcon)
                putExtra(DownloadConst.KEY_FILE_NAME, fileName)
                putExtra(DownloadConst.KEY_LENGTH, totalLength)
                putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
                putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
                action = NotificationConst.ACTION_NOTIFICATION_COMPLETED
            }
        )
    }

    fun sendDownloadFailedNotification() {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtra(NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME, notificationChannelName)
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_IMPORTANCE,
                    notificationImportance
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_DESCRIPTION,
                    notificationChannelDescription
                )
                putExtra(NotificationConst.KEY_NOTIFICATION_SMALL_ICON, notificationSmallIcon)
                putExtra(DownloadConst.KEY_FILE_NAME, fileName)
                putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
                putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
                action = NotificationConst.ACTION_NOTIFICATION_FAILED
            }
        )
    }

    fun sendDownloadCancelledNotification() {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtra(NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME, notificationChannelName)
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_IMPORTANCE,
                    notificationImportance
                )
                putExtra(
                    NotificationConst.KEY_NOTIFICATION_CHANNEL_DESCRIPTION,
                    notificationChannelDescription
                )
                putExtra(NotificationConst.KEY_NOTIFICATION_SMALL_ICON, notificationSmallIcon)
                putExtra(DownloadConst.KEY_FILE_NAME, fileName)
                putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
                putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
                action = NotificationConst.ACTION_NOTIFICATION_CANCELLED
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NotificationConst.NOTIFICATION_CHANNEL_ID,
            notificationChannelName,
            notificationImportance
        )
        channel.description = notificationChannelDescription
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun getNotificationId(): Int = notificationId
}