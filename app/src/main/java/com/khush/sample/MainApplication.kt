package com.khush.sample

import android.app.Application
import com.ketch.DownloadConfig
import com.ketch.Ketch
import com.ketch.NotificationConfig
import com.ketch.NotificationSmallIconConfig

class MainApplication : Application() {

    lateinit var ketch: Ketch

    override fun onCreate() {
        super.onCreate()
        ketch = Ketch.builder()
            .setDownloadConfig(DownloadConfig())
            .setNotificationConfig(
                NotificationConfig(
                    true,
                    smallIcon = R.drawable.ic_launcher_foreground,
                    smallIcons = NotificationSmallIconConfig(
                        progress = android.R.drawable.stat_sys_download,
                        success = android.R.drawable.checkbox_on_background,
                        failed = android.R.drawable.stat_notify_error,
                        paused = android.R.drawable.ic_media_pause,
                        cancelled = android.R.drawable.ic_notification_clear_all
                    )
                )
            )
            .enableLogs(true)
            .build(this)
    }

}