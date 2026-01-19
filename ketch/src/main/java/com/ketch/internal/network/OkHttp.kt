/*
 * Copyright (c) 2022 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.ketch.internal.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.Response
import java.io.Closeable
import java.io.IOException
import kotlin.coroutines.resumeWithException

suspend fun Call.executeAsync(): Response =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            this.cancel()
        }
        this.enqueue(
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(
                    call: Call,
                    response: Response,
                ) {
                    continuation.resume(response) { _, value, _ ->
                        value.closeQuietly()
                    }
                }
            },
        )
    }

/**
 * Closes `closeable`, ignoring any checked exceptions. Does nothing if `closeable` is
 * null.
 */
internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (rethrown: RuntimeException) {
        throw rethrown
    } catch (_: Exception) {
    }
}

// https://github.com/square/retrofit/blob/7070c35e1afdca59bb30569125ddd0ca5d8a5758/retrofit/src/main/java/retrofit2/RequestBuilder.java#L193
internal fun HttpUrl.requireResolve(link: String): HttpUrl {
    return requireNotNull(resolve(link)) {
        "Malformed URL. Base: $this, Relative: $link"
    }
}
