package com.ketch.internal.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ketch.Ketch
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.NotificationConst
import com.ketch.internal.utils.TextUtil

/**
 * Notification receiver: Responsible for showing the terminating state notification (paused, cancelled, failed)
 * It also handles the user action from notification (Pause, Resume, Cancel, Retry)
 *
 * Notification ID = (Unique Download Request ID + 1) for each download
 *
 * @constructor Create empty Notification receiver
 */
internal class NotificationReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {

        if (context == null || intent == null) return
        val ketch = Ketch.builder().build(context)

        // Resume the download and dismiss the notification
        if (intent.action == NotificationConst.ACTION_NOTIFICATION_RESUME_CLICK) {
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID)
            val nId = intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
            if (nId != null) NotificationManagerCompat.from(context).cancel(nId)
            if (requestId != null) {
                ketch.resume(requestId)
            }
            return
        }

        // Retry the download and dismiss the notification
        if (intent.action == NotificationConst.ACTION_NOTIFICATION_RETRY_CLICK) {
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID)
            val nId = intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
            if (nId != null) NotificationManagerCompat.from(context).cancel(nId)
            if (requestId != null) {
                ketch.retry(requestId)
            }
            return
        }

        // Pause the download and dismiss the notification
        if (intent.action == NotificationConst.ACTION_NOTIFICATION_PAUSE_CLICK) {
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID)
            val nId = intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
            if (nId != null) NotificationManagerCompat.from(context).cancel(nId)
            if (requestId != null) {
                ketch.pause(requestId)
            }
            return
        }

        // Cancel the download and dismiss the notification
        if (intent.action == NotificationConst.ACTION_NOTIFICATION_CANCEL_CLICK) {
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID)
            val nId = intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
            if (nId != null) NotificationManagerCompat.from(context).cancel(nId)
            if (requestId != null) {
                ketch.cancel(requestId)
            }
            return
        }

        // List of actions when notification gets triggered
        val notificationActionList = listOf(
            NotificationConst.ACTION_DOWNLOAD_COMPLETED,
            NotificationConst.ACTION_DOWNLOAD_FAILED,
            NotificationConst.ACTION_DOWNLOAD_CANCELLED,
            NotificationConst.ACTION_DOWNLOAD_PAUSED
        )

        if (intent.action in notificationActionList) {
            val notificationChannelName =
                intent.extras?.getString(NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME)
                    ?: NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_NAME
            val notificationImportance =
                intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_CHANNEL_IMPORTANCE)
                    ?: NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_IMPORTANCE
            val notificationChannelDescription =
                intent.extras?.getString(NotificationConst.KEY_NOTIFICATION_CHANNEL_DESCRIPTION)
                    ?: NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_DESCRIPTION
            val notificationSmallIcon =
                intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_SMALL_ICON)
                    ?: NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON
            val fileName = intent.extras?.getString(DownloadConst.KEY_FILE_NAME) ?: ""
            val currentProgress = intent.extras?.getInt(DownloadConst.KEY_PROGRESS) ?: 0
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID) ?: -1
            val totalLength = intent.extras?.getLong(DownloadConst.KEY_LENGTH)
                ?: DownloadConst.DEFAULT_VALUE_LENGTH

            val notificationId = requestId + 1 // unique id for the notification

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(
                    context = context,
                    notificationChannelName = notificationChannelName,
                    notificationImportance = notificationImportance,
                    notificationChannelDescription = notificationChannelDescription
                )
            }

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

            // Resume Notification
            val intentResume = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationConst.ACTION_NOTIFICATION_RESUME_CLICK
            }
            intentResume.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
            intentResume.putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
            val pendingIntentResume = PendingIntent.getBroadcast(
                context.applicationContext,
                notificationId,
                intentResume,
                PendingIntent.FLAG_IMMUTABLE
            )

            // Retry Notification
            val intentRetry = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationConst.ACTION_NOTIFICATION_RETRY_CLICK
            }
            intentRetry.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
            intentRetry.putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
            val pendingIntentRetry =
                PendingIntent.getBroadcast(
                    context.applicationContext,
                    notificationId,
                    intentRetry,
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

            var notificationBuilder =
                NotificationCompat.Builder(context, NotificationConst.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(notificationSmallIcon)
                    .setContentText(
                        when (intent.action) {
                            NotificationConst.ACTION_DOWNLOAD_COMPLETED ->
                                "Download successful. (${
                                    TextUtil.getTotalLengthText(
                                        totalLength
                                    )
                                })"

                            NotificationConst.ACTION_DOWNLOAD_FAILED -> "Download failed."
                            NotificationConst.ACTION_DOWNLOAD_PAUSED -> "Download paused."
                            else -> "Download cancelled."
                        }
                    )
                    .setContentTitle(fileName)
                    .setContentIntent(pendingIntentOpen)
                    .setOnlyAlertOnce(true)
                    .setOngoing(false)
                    .setAutoCancel(true)

            // add retry and cancel button for failed download
            if (intent.action == NotificationConst.ACTION_DOWNLOAD_FAILED) {
                notificationBuilder = notificationBuilder.addAction(
                    -1,
                    NotificationConst.RETRY_BUTTON_TEXT,
                    pendingIntentRetry
                )
                    .setProgress(DownloadConst.MAX_VALUE_PROGRESS, currentProgress, false)
                    .addAction(-1, NotificationConst.CANCEL_BUTTON_TEXT, pendingIntentCancel)
                    .setSubText("$currentProgress%")
            }
            // add resume and cancel button for paused download
            if (intent.action == NotificationConst.ACTION_DOWNLOAD_PAUSED) {
                notificationBuilder = notificationBuilder.addAction(
                    -1,
                    NotificationConst.RESUME_BUTTON_TEXT,
                    pendingIntentResume
                )
                    .setProgress(DownloadConst.MAX_VALUE_PROGRESS, currentProgress, false)
                    .addAction(-1, NotificationConst.CANCEL_BUTTON_TEXT, pendingIntentCancel)
                    .setSubText("$currentProgress%")
            }

            val notification = notificationBuilder
                .build()

            NotificationManagerCompat.from(context).notify(
                notificationId,
                notification
            )
        }
    }

    /**
     * Create notification channel for File downloads
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        context: Context,
        notificationChannelName: String,
        notificationImportance: Int,
        notificationChannelDescription: String
    ) {
        val channel = NotificationChannel(
            NotificationConst.NOTIFICATION_CHANNEL_ID,
            notificationChannelName,
            notificationImportance
        )
        channel.description = notificationChannelDescription
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
