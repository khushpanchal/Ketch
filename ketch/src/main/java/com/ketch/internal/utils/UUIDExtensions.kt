package com.ketch.internal.utils

import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.UUID
import kotlin.experimental.and

internal fun UUID.toUniqueId(): Int {
    val hash: ByteArray = try {
        MessageDigest.getInstance("MD5").digest(this.toString().toByteArray(charset("UTF-8")))
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException("NoSuchAlgorithmException", e)
    } catch (e: UnsupportedEncodingException) {
        throw RuntimeException("UnsupportedEncodingException", e)
    }
    val hex = StringBuilder(hash.size * 2)
    for (b in hash) {
        if (b and 0xFF.toByte() < 0x10) hex.append("0")
        hex.append(Integer.toHexString((b and 0xFF.toByte()).toInt()))
    }
    return hex.toString().hashCode()
}