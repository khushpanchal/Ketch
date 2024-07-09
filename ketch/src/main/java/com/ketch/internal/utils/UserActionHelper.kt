package com.ketch.internal.utils

//todo Use DB
internal object UserActionHelper {

    private val idUserActionMap = hashMapOf<Int, String>()

    fun setUserAction(id: Int, action: UserAction) {
        idUserActionMap[id] = action.toString()
    }

    fun getUserAction(id: Int): String? {
        return idUserActionMap[id]
    }
}