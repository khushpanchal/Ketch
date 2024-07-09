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
import com.ketch.internal.utils.NotificationHelper
import com.ketch.internal.utils.TextUtil

internal class NotificationReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == NotificationConst.ACTION_NOTIFICATION_DISMISSED) {
            val dismissedId = intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
            if (dismissedId != null) {
                NotificationHelper.addToDismissedNotificationIds(dismissedId)
            }
            return
        }

        if (context == null || intent == null) return

        if(intent.action == NotificationConst.ACTION_NOTIFICATION_RESUME_CLICK) {
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID) ?: return
            val nId = intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
            if(nId != null) NotificationManagerCompat.from(context).cancel(nId)
            Ketch.getInstance(context).resume(requestId)
            return
        }

        if(intent.action == NotificationConst.ACTION_NOTIFICATION_RETRY_CLICK) {
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID) ?: return
            val nId = intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
            if(nId != null) NotificationManagerCompat.from(context).cancel(nId)
            Ketch.getInstance(context).retry(requestId)
            return
        }

        if(intent.action == NotificationConst.ACTION_NOTIFICATION_PAUSE_CLICK) {
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID) ?: return
            val nId = intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
            if(nId != null) NotificationManagerCompat.from(context).cancel(nId)
            Ketch.getInstance(context).pause(requestId)
            return
        }

        if(intent.action == NotificationConst.ACTION_NOTIFICATION_CANCEL_CLICK) {
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID) ?: return
            val nId = intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
            if(nId != null) NotificationManagerCompat.from(context).cancel(nId)
            Ketch.getInstance(context).cancel(requestId)
            return
        }

        if (intent.action == NotificationConst.ACTION_DOWNLOAD_COMPLETED
            || intent.action == NotificationConst.ACTION_DOWNLOAD_FAILED
            || intent.action == NotificationConst.ACTION_DOWNLOAD_CANCELLED
            || intent.action == NotificationConst.ACTION_DOWNLOAD_PAUSED
        ) {
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
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID) ?: -1
            val totalLength = intent.extras?.getLong(DownloadConst.KEY_LENGTH)
                ?: DownloadConst.DEFAULT_VALUE_LENGTH

            val notificationId = ((requestId + 1 + System.currentTimeMillis()) % Int.MAX_VALUE).toInt() //Unique

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(
                    context = context,
                    notificationChannelName = notificationChannelName,
                    notificationImportance = notificationImportance,
                    notificationChannelDescription = notificationChannelDescription
                )
            }

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


            val intentResume = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationConst.ACTION_NOTIFICATION_RESUME_CLICK
            }
            intentResume.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
            intentResume.putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
            val pendingIntentResume = PendingIntent.getBroadcast(
                context.applicationContext, notificationId, intentResume,
                PendingIntent.FLAG_IMMUTABLE
            )

            val intentRetry = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationConst.ACTION_NOTIFICATION_RETRY_CLICK
            }
            intentRetry.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
            intentRetry.putExtra(DownloadConst.KEY_REQUEST_ID, requestId)
            val pendingIntentRetry = PendingIntent.getBroadcast(
                context.applicationContext, notificationId, intentRetry,
                PendingIntent.FLAG_IMMUTABLE
            )

            var notificationBuilder =
                NotificationCompat.Builder(context, NotificationConst.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(notificationSmallIcon)
                    .setContentText(
                        when (intent.action) {
                            NotificationConst.ACTION_DOWNLOAD_COMPLETED -> "Download successful. (${
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

            if(intent.action == NotificationConst.ACTION_DOWNLOAD_FAILED) {
                notificationBuilder = notificationBuilder.addAction(-1, NotificationConst.RETRY_BUTTON_TEXT, pendingIntentRetry)
            }
            if(intent.action == NotificationConst.ACTION_DOWNLOAD_PAUSED) {
                notificationBuilder = notificationBuilder.addAction(-1, NotificationConst.RESUME_BUTTON_TEXT, pendingIntentResume)
            }

            val notification = notificationBuilder
                .build()

            NotificationManagerCompat.from(context).notify(
                notificationId,
                notification
            )
        }
    }

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