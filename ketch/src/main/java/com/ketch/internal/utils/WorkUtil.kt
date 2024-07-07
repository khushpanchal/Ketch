package com.ketch.internal.utils

import com.ketch.internal.worker.WorkInputData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal object WorkUtil {

    fun WorkInputData.toJson(): String {
        return Gson().toJson(this)
    }

    fun fromJson(jsonStr: String): WorkInputData {
        return Gson().fromJson(jsonStr, WorkInputData::class.java)
    }

    fun hashMapToJson(headers: HashMap<String, String>): String {
        val gson = Gson()
        return gson.toJson(headers)
    }

    fun jsonToHashMap(jsonString: String): HashMap<String, String> {
        val gson = Gson()
        val type = object : TypeToken<HashMap<String, String>>() {}.type
        return gson.fromJson(jsonString, type)
    }
}