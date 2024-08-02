package com.ketch

/**
 * Download model: Data class sent to client mapped to each download
 *
 * @property url Download URL sent by client
 * @property path Download Path sent by client
 * @property fileName Name of the file sent by client
 * @property tag Optional tag for each download to group the download into category
 * @property id Unique download id created by the combination of url, path and filename
 * @property headers Optional headers sent when making api call for file download
 * @property timeQueued First time in millisecond when download was queued into the database
 * @property status Current [Status] of the download
 * @property total Total size of file in bytes
 * @property progress Current download progress in Int between 0 and 100
 * @property speedInBytePerMs Current speed of download in bytes per second
 * @property lastModified Last modified time of database for current download (any change update the time)
 * @property eTag ETag of the download file sent by API response headers
 * @property metaData Optional metaData set by client for adding any extra download info
 * @property failureReason Failure reason for failed download
 * @constructor Create empty Download model
 */
data class DownloadModel(
    val url: String,
    val path: String,
    val fileName: String,
    val tag: String,
    val id: Int,
    val headers: HashMap<String, String>,
    val timeQueued: Long,
    val status: Status,
    val total: Long,
    val progress: Int,
    val speedInBytePerMs: Float,
    val lastModified: Long,
    val eTag: String,
    val metaData: String,
    val failureReason: String
)
