package com.ketch.internal.utils

internal object DownloadConst {
    const val DEFAULT_VALUE_READ_TIMEOUT_MS = 10000L
    const val DEFAULT_VALUE_CONNECT_TIMEOUT_MS = 10000L
    const val BASE_URL = "http://localhost/"
    const val TAG_DOWNLOAD = "downloads"
    const val KEY_FILE_NAME = "key_fileName"
    const val KEY_STATE = "key_state"
    const val KEY_PROGRESS = "key_progress"
    const val MAX_VALUE_PROGRESS = 100
    const val PROGRESS = "progress"
    const val STARTED = "started"
    const val KEY_LENGTH = "key_length"
    const val DEFAULT_VALUE_LENGTH = 0L
    const val KEY_REQUEST_ID = "key_request_id"
    const val KEY_DOWNLOAD_REQUEST = "key_download_request"
    const val KEY_NOTIFICATION_CONFIG = "key_notification_config"
    const val ETAG_HEADER = "ETag"
    const val CONTENT_LENGTH = "Content-Length"
    const val RANGE_HEADER = "Range"
    const val HTTP_RANGE_NOT_SATISFY = 416
}
