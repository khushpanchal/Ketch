package com.ketch

import java.io.IOException

sealed class KetchException(message: String) : IOException(message) {
    class NonRetryableException(message: String) : KetchException(message)
    class RetryableException(message: String) : KetchException(message)
}
