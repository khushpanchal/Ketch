package com.ketch.internal.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ketch.Status
import com.ketch.internal.utils.DbConst
import com.ketch.internal.utils.UserAction

@Entity(
    tableName = DbConst.DB_TABLE_NAME,
    indices = [Index(
        value = [DbConst.ID],
        unique = true
    )]
)
internal data class DownloadEntity(
    @ColumnInfo(DbConst.URL)
    var url: String = "",

    @ColumnInfo(DbConst.PATH)
    var path: String = "",

    @ColumnInfo(DbConst.FILE_NAME)
    var fileName: String = "",

    @ColumnInfo(DbConst.TAG)
    var tag: String = "",

    @PrimaryKey
    @ColumnInfo(DbConst.ID)
    var id: Int = 0,

    @ColumnInfo(DbConst.HEADERS_JSON)
    var headersJson: String = "",

    @ColumnInfo(DbConst.TIME_QUEUE)
    var timeQueued: Long = 0,

    @ColumnInfo(DbConst.STATUS)
    var status: String = Status.DEFAULT.toString(),

    @ColumnInfo(DbConst.TOTAL_BYTES)
    var totalBytes: Long = 0,

    @ColumnInfo(DbConst.DOWNLOADED_BYTES)
    var downloadedBytes: Long = 0,

    @ColumnInfo(DbConst.SPEED_BPS)
    var speedInBytePerMs: Float = 0f,

    @ColumnInfo(DbConst.UUID)
    var uuid: String = "",

    @ColumnInfo(DbConst.LAST_MODIFIED)
    var lastModified: Long = 0,

    @ColumnInfo(DbConst.E_TAG)
    var eTag: String = "",

    @ColumnInfo(DbConst.USER_ACTION)
    var userAction: String = UserAction.DEFAULT.toString(),

    @ColumnInfo(DbConst.META_DATA)
    var metaData: String = "",

    @ColumnInfo(DbConst.FAILURE_REASON)
    var failureReason: String = ""
)
