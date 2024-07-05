package com.ketch.internal.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ketch.internal.utils.DbConst

class DatabaseOpenHelper internal constructor(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS " +
                    DbHelperImpl.TABLE_NAME + "( " +
                    DbConst.ID + " INTEGER PRIMARY KEY, " +
                    DbConst.URL + " VARCHAR, " +
                    DbConst.PATH + " VARCHAR, " +
                    DbConst.FILE_NAME + " VARCHAR, " +
                    DbConst.TOTAL_BYTES + " BIGINT, " +
                    DbConst.DOWNLOADED_BYTES + " BIGINT, " +
                    DbConst.E_TAG + " VARCHAR, " +
                    DbConst.UUID + " VARCHAR, " +
                    DbConst.STATUS + " VARCHAR, " +
                    DbConst.TIME_QUEUE + " BIGINT, " +
                    DbConst.LAST_MODIFIED + " BIGINT, " +
                    DbConst.HEADERS_JSON + " VARCHAR, " +
                    DbConst.CONFIG_JSON + " VARCHAR, " +
                    DbConst.TAG + " VARCHAR " +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {}

    companion object {
        private const val DATABASE_NAME = "downloader.db"
        private const val DATABASE_VERSION = 1
    }
}