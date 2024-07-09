package com.khush.sample

import android.app.Application
import com.ketch.Ketch
import com.ketch.NotificationConfig

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Ketch.getInstance(this).initConfig(
            notificationConfig = NotificationConfig(
                enabled = true,
                smallIcon = R.drawable.ic_launcher_foreground
            )
        )
    }

}