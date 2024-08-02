package com.ketch.internal.database

import android.content.Context
import androidx.room.Room

internal object DatabaseInstance {

    @Volatile
    private var INSTANCE: DownloadDatabase? = null

    fun getInstance(context: Context): DownloadDatabase {
        if (INSTANCE == null) {
            synchronized(DownloadDatabase::class) {
                if (INSTANCE == null) {
                    INSTANCE = buildRoomDB(context)
                }
            }
        }
        return INSTANCE!!
    }

    private fun buildRoomDB(context: Context) =
        Room.databaseBuilder(
            context.applicationContext,
            DownloadDatabase::class.java,
            "ketch_downloader"
        ).fallbackToDestructiveMigration().build()
}
