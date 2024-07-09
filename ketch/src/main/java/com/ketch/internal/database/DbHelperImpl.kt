package com.ketch.internal.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.ketch.internal.utils.DbConst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DbHelperImpl(context: Context): DbHelper {

    private var db: SQLiteDatabase

    companion object {
        const val TABLE_NAME = "downloads"
    }

    init {
        val databaseOpenHelper = DatabaseOpenHelper(context)
        db = databaseOpenHelper.writableDatabase
    }

    @SuppressLint("Range")
    override suspend fun find(id: Int): DownloadEntity? {
        var downloadEntity: DownloadEntity? = null
        val cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_NAME + " WHERE " + DbConst.ID + " = " + id,
            null
        )
        if (cursor != null && cursor.moveToFirst()) {
            downloadEntity = DownloadEntity(
                id = id,
                url = cursor.getString(cursor.getColumnIndex(DbConst.URL)),
                path = cursor.getString(cursor.getColumnIndex(DbConst.PATH)),
                fileName = cursor.getString(cursor.getColumnIndex(DbConst.FILE_NAME)),
                totalBytes = cursor.getLong(cursor.getColumnIndex(DbConst.TOTAL_BYTES)),
                downloadedBytes = cursor.getLong(cursor.getColumnIndex(DbConst.DOWNLOADED_BYTES)),
                eTag = cursor.getString(cursor.getColumnIndex(DbConst.E_TAG)),
                uuid = cursor.getString(cursor.getColumnIndex(DbConst.UUID)),
                status = cursor.getString(cursor.getColumnIndex(DbConst.STATUS)),
                timeQueued = cursor.getLong(cursor.getColumnIndex(DbConst.TIME_QUEUE)),
                lastModified = cursor.getLong(cursor.getColumnIndex(DbConst.LAST_MODIFIED)),
                headersJson = cursor.getString(cursor.getColumnIndex(DbConst.HEADERS_JSON)),
                tag = cursor.getString(cursor.getColumnIndex(DbConst.TAG)),
            )
        }
        cursor.close()
        return downloadEntity
    }

    override suspend fun insert(entity: DownloadEntity) {
        val values = ContentValues()
        values.put(DbConst.ID, entity.id)
        values.put(DbConst.URL, entity.url)
        values.put(DbConst.PATH, entity.path)
        values.put(DbConst.FILE_NAME, entity.fileName)
        values.put(DbConst.TOTAL_BYTES, entity.totalBytes)
        values.put(DbConst.DOWNLOADED_BYTES, entity.downloadedBytes)
        values.put(DbConst.E_TAG, entity.eTag)
        values.put(DbConst.UUID, entity.uuid)
        values.put(DbConst.STATUS, entity.status)
        values.put(DbConst.TIME_QUEUE, entity.timeQueued)
        values.put(DbConst.LAST_MODIFIED, entity.lastModified)
        values.put(DbConst.HEADERS_JSON, entity.headersJson)
        values.put(DbConst.TAG, entity.tag)

        try {
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_NONE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun update(entity: DownloadEntity) {
        try {
            val values = ContentValues()
            values.put(DbConst.URL, entity.url)
            values.put(DbConst.PATH, entity.path)
            values.put(DbConst.FILE_NAME, entity.fileName)
            values.put(DbConst.TOTAL_BYTES, entity.totalBytes)
            values.put(DbConst.DOWNLOADED_BYTES, entity.downloadedBytes)
            values.put(DbConst.E_TAG, entity.eTag)
            values.put(DbConst.UUID, entity.uuid)
            values.put(DbConst.STATUS, entity.status)
            values.put(DbConst.TIME_QUEUE, entity.timeQueued)
            values.put(DbConst.LAST_MODIFIED, entity.lastModified)
            values.put(DbConst.HEADERS_JSON, entity.headersJson)
            values.put(DbConst.TAG, entity.tag)
            db.update(
                TABLE_NAME,
                values,
                DbConst.ID + " = ? ",
                arrayOf((entity.id).toString())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun updateProgress(id: Int, downloadedBytes: Long, lastModifiedAt: Long) {
        withContext(Dispatchers.IO) {
            try {
                val values = ContentValues()
                values.put(DbConst.DOWNLOADED_BYTES, downloadedBytes)
                db.update(
                    TABLE_NAME,
                    values,
                    DbConst.ID + " = ? ",
                    arrayOf("$id")
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun remove(id: Int) {
        try {
            db.execSQL(
                "DELETE FROM " + TABLE_NAME + " WHERE " +
                        DbConst.ID + " = " + id
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun updateStatus(id: Int, status: String, lastModifiedAt: Long) {
        try {
            val values = ContentValues()
            values.put(DbConst.STATUS, status)
            db.update(
                TABLE_NAME,
                values,
                DbConst.ID + " = ? ",
                arrayOf("$id")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun empty() {
        try {
            db.execSQL("DELETE * FROM " + TABLE_NAME)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("Range")
    override suspend fun getAllEntity(): List<DownloadEntity> {
        val entities: MutableList<DownloadEntity> = ArrayList()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val entity = DownloadEntity()
                    entity.id = (cursor.getInt(cursor.getColumnIndex(DbConst.ID)))
                    entity.url = (cursor.getString(cursor.getColumnIndex(DbConst.URL)))
                    entity.path =
                        (cursor.getString(cursor.getColumnIndex(DbConst.PATH)))
                    entity.fileName =
                        (cursor.getString(cursor.getColumnIndex(DbConst.FILE_NAME)))
                    entity.totalBytes =
                        (cursor.getLong(cursor.getColumnIndex(DbConst.TOTAL_BYTES)))
                    entity.downloadedBytes =
                        (cursor.getLong(cursor.getColumnIndex(DbConst.DOWNLOADED_BYTES)))
                    entity.eTag =
                        (cursor.getString(cursor.getColumnIndex(DbConst.E_TAG)))
                    entity.uuid =
                        (cursor.getString(cursor.getColumnIndex(DbConst.UUID)))
                    entity.status =
                        (cursor.getString(cursor.getColumnIndex(DbConst.STATUS)))
                    entity.timeQueued =
                        (cursor.getLong(cursor.getColumnIndex(DbConst.TIME_QUEUE)))
                    entity.lastModified =
                        (cursor.getLong(cursor.getColumnIndex(DbConst.LAST_MODIFIED)))
                    entity.headersJson =
                        (cursor.getString(cursor.getColumnIndex(DbConst.HEADERS_JSON)))
                    entity.tag =
                        (cursor.getString(cursor.getColumnIndex(DbConst.TAG)))
                    entities.add(entity)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            //
        } finally {
            cursor?.close()
        }
        return entities
    }

    @SuppressLint("Range")
    override suspend fun getEntityTillTime(timeMillis: Long): List<DownloadEntity> {
        val entities: MutableList<DownloadEntity> = ArrayList()
        var cursor: Cursor? = null
        try {
            val beforeTimeInMillis = System.currentTimeMillis() - timeMillis
            cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " +
                        DbConst.LAST_MODIFIED + " <= " + beforeTimeInMillis, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val entity = DownloadEntity()
                    entity.id = (cursor.getInt(cursor.getColumnIndex(DbConst.ID)))
                    entity.url = (cursor.getString(cursor.getColumnIndex(DbConst.URL)))
                    entity.path =
                        (cursor.getString(cursor.getColumnIndex(DbConst.PATH)))
                    entity.fileName =
                        (cursor.getString(cursor.getColumnIndex(DbConst.FILE_NAME)))
                    entity.totalBytes =
                        (cursor.getLong(cursor.getColumnIndex(DbConst.TOTAL_BYTES)))
                    entity.downloadedBytes =
                        (cursor.getLong(cursor.getColumnIndex(DbConst.DOWNLOADED_BYTES)))
                    entity.eTag =
                        (cursor.getString(cursor.getColumnIndex(DbConst.E_TAG)))
                    entity.uuid =
                        (cursor.getString(cursor.getColumnIndex(DbConst.UUID)))
                    entity.status =
                        (cursor.getString(cursor.getColumnIndex(DbConst.STATUS)))
                    entity.timeQueued =
                        (cursor.getLong(cursor.getColumnIndex(DbConst.TIME_QUEUE)))
                    entity.lastModified =
                        (cursor.getLong(cursor.getColumnIndex(DbConst.LAST_MODIFIED)))
                    entity.headersJson =
                        (cursor.getString(cursor.getColumnIndex(DbConst.HEADERS_JSON)))
                    entity.tag =
                        (cursor.getString(cursor.getColumnIndex(DbConst.TAG)))
                    entities.add(entity)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            //
        } finally {
            cursor?.close()
        }
        return entities
    }

}