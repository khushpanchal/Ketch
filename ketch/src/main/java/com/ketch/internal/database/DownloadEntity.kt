package com.ketch.internal.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ketch.Status
import com.ketch.internal.utils.UserAction

@Entity(
    tableName = "downloads"
)
internal data class DownloadEntity(
    var url: String = "",
    var path: String = "",
    var fileName: String = "",
    var tag: String = "",
    @PrimaryKey
    var id: Int = 0,
    var headersJson: String = "",
    var timeQueued: Long = 0,
    var status: String = Status.DEFAULT.toString(),
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var speedInBytePerMs: Float = 0f,
    var uuid: String = "",
    var lastModified: Long = 0,
    var eTag: String = "",
    var userAction: String = UserAction.DEFAULT.toString(),
    var metaData: String = "",
    var failureReason: String = ""
)
