package com.khush.sample

import android.app.Application
import com.ketch.DownloadConfig
import com.ketch.Ketch
import com.ketch.NotificationConfig

class MainApplication : Application() {

    lateinit var ketch: Ketch

    override fun onCreate() {
        super.onCreate()
        ketch = Ketch.builder()
            .setDownloadConfig(DownloadConfig())
            .setNotificationConfig(
                NotificationConfig(
                    true,
                    smallIcon = R.drawable.ic_launcher_foreground
                )
            )
            .enableLogs(true)
            .build(this)
    }

}