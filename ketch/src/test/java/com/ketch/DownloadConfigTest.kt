package com.ketch

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DownloadConfigTest {

    @Test
    fun `default autoRetry is true`() {
        val config = DownloadConfig()
        assertThat(config.autoRetry).isTrue()
    }
}
