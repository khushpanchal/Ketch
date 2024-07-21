package com.ketch.internal.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ketch.internal.utils.DbConst
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("SELECT * FROM ${DbConst.DB_TABLE_NAME} WHERE ${DbConst.ID} = :id")
    suspend fun find(id: Int): DownloadEntity?

    @Query("UPDATE ${DbConst.DB_TABLE_NAME} SET ${DbConst.DOWNLOADED_BYTES} = :downloadedBytes, ${DbConst.LAST_MODIFIED} = :lastModifiedAt WHERE id = :id")
    suspend fun updateProgress(id: Int, downloadedBytes: Long, lastModifiedAt: Long)

    @Query("UPDATE ${DbConst.DB_TABLE_NAME} SET ${DbConst.STATUS} = :status, ${DbConst.LAST_MODIFIED} = :lastModifiedAt WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String, lastModifiedAt: Long)

    @Query("UPDATE ${DbConst.DB_TABLE_NAME} SET ${DbConst.USER_ACTION} = :userAction, ${DbConst.LAST_MODIFIED} = :lastModifiedAt WHERE id = :id")
    suspend fun updateUserAction(id: Int, userAction: String, lastModifiedAt: Long)

    @Query("UPDATE ${DbConst.DB_TABLE_NAME} SET ${DbConst.UUID} = :uuid, ${DbConst.LAST_MODIFIED} = :lastModifiedAt WHERE id = :id")
    suspend fun updateUuid(id: Int, uuid: String, lastModifiedAt: Long)

    @Query("UPDATE ${DbConst.DB_TABLE_NAME} SET ${DbConst.FAILURE_REASON} = :failureReason, ${DbConst.LAST_MODIFIED} = :lastModifiedAt WHERE ${DbConst.ID} = :id")
    suspend fun updateFailureReason(id: Int, failureReason: String, lastModifiedAt: Long)

    @Query("UPDATE ${DbConst.DB_TABLE_NAME} SET ${DbConst.E_TAG} = :eTag, ${DbConst.LAST_MODIFIED} = :lastModifiedAt WHERE ${DbConst.ID} = :id")
    suspend fun updateETag(id: Int, eTag: String, lastModifiedAt: Long)

    @Query("UPDATE ${DbConst.DB_TABLE_NAME} SET ${DbConst.TOTAL_BYTES} = :totalLength, ${DbConst.LAST_MODIFIED} = :lastModifiedAt WHERE ${DbConst.ID} = :id")
    suspend fun updateTotalLength(id: Int, totalLength: Long, lastModifiedAt: Long)

    @Query("UPDATE ${DbConst.DB_TABLE_NAME} SET ${DbConst.SPEED_BPS} = :speed, ${DbConst.LAST_MODIFIED} = :lastModifiedAt WHERE ${DbConst.ID} = :id")
    suspend fun updateSpeed(id: Int, speed: Float, lastModifiedAt: Long)

    @Query("DELETE FROM ${DbConst.DB_TABLE_NAME} WHERE ${DbConst.ID} = :id")
    suspend fun remove(id: Int)

    @Query("DELETE FROM ${DbConst.DB_TABLE_NAME}")
    suspend fun empty()

    @Query("SELECT * FROM ${DbConst.DB_TABLE_NAME}")
    fun getAllEntityFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM ${DbConst.DB_TABLE_NAME} WHERE ${DbConst.LAST_MODIFIED} <= :timeMillis")
    fun getEntityTillTimeFlow(timeMillis: Long): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM ${DbConst.DB_TABLE_NAME} WHERE ${DbConst.TAG} = :tag")
    fun getAllEntityByTagFlow(tag: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM ${DbConst.DB_TABLE_NAME} WHERE ${DbConst.ID} = :id")
    fun getEntityByIdFlow(id: Int): Flow<DownloadEntity>

    @Query("SELECT * FROM ${DbConst.DB_TABLE_NAME}")
    suspend fun getAllEntity(): List<DownloadEntity>

    @Query("SELECT * FROM ${DbConst.DB_TABLE_NAME} WHERE ${DbConst.LAST_MODIFIED} <= :timeMillis")
    suspend fun getEntityTillTime(timeMillis: Long): List<DownloadEntity>

    @Query("SELECT * FROM ${DbConst.DB_TABLE_NAME} WHERE ${DbConst.TAG} = :tag")
    suspend fun getAllEntityByTag(tag: String): List<DownloadEntity>

    @Query("SELECT * FROM ${DbConst.DB_TABLE_NAME} WHERE ${DbConst.ID} = :id")
    suspend fun getEntityById(id: Int): DownloadEntity
}
