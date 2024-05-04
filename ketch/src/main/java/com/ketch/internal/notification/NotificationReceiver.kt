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
        if (intent.action == NotificationConst.ACTION_NOTIFICATION_COMPLETED
            || intent.action == NotificationConst.ACTION_NOTIFICATION_FAILED
            || intent.action == NotificationConst.ACTION_NOTIFICATION_CANCELLED
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
            val notificationId =
                intent.extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID) ?: -1
            val requestId =
                intent.extras?.getInt(DownloadConst.KEY_REQUEST_ID) ?: -1
            val totalLength = intent.extras?.getLong(DownloadConst.KEY_LENGTH)
                ?: DownloadConst.DEFAULT_VALUE_LENGTH

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
                    (notificationId + 1) % Int.MAX_VALUE,
                    intentOpen,
                    PendingIntent.FLAG_IMMUTABLE
                )


            val notification =
                NotificationCompat.Builder(context, NotificationConst.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(notificationSmallIcon)
                    .setContentText(
                        when (intent.action) {
                            NotificationConst.ACTION_NOTIFICATION_COMPLETED -> "Download successful. (${
                                TextUtil.getTotalLengthText(
                                    totalLength
                                )
                            })"

                            NotificationConst.ACTION_NOTIFICATION_FAILED -> "Download failed."
                            else -> "Download cancelled."
                        }
                    )
                    .setContentTitle(fileName)
                    .setContentIntent(pendingIntentOpen)
                    .setOnlyAlertOnce(true)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .build()

            NotificationManagerCompat.from(context).notify(
                (notificationId + 1) % Int.MAX_VALUE, //Unique Id
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