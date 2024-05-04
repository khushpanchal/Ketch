package com.ketch.internal.utils

import com.ketch.internal.worker.WorkInputData
import com.google.gson.Gson

internal object WorkUtil {

    fun WorkInputData.toJson(): String {
        return Gson().toJson(this)
    }

    fun fromJson(jsonStr: String): WorkInputData {
        return Gson().fromJson(jsonStr, WorkInputData::class.java)
    }
}