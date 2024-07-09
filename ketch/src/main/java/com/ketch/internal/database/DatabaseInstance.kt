package com.ketch.internal.database

import android.content.Context

internal object DatabaseInstance {

    @Volatile
    private var dbHelper: DbHelper? = null

    fun getDbHelper(context: Context): DbHelper {
        if(dbHelper == null) {
            synchronized(this) {
                if(dbHelper==null) {
                    dbHelper = DbHelperImpl(context)
                }
            }
        }
        return dbHelper!!
    }

}