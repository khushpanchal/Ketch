package com.ketch.internal.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ketch.internal.download.DownloadTask
import com.ketch.internal.network.RetrofitInstance
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.ExceptionConst
import com.ketch.internal.utils.WorkUtil

internal class DownloadWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) :
    CoroutineWorker(context, workerParameters) {


    override suspend fun doWork(): Result {

        val workInputData =
            WorkUtil.fromJson(
                inputData.getString(DownloadConst.KEY_WORK_INPUT_DATA)
                    ?: return Result.failure(
                        workDataOf(ExceptionConst.KEY_EXCEPTION to ExceptionConst.EXCEPTION_FAILED_DESERIALIZE)
                    )
            )

        val id = workInputData.id
        val url = workInputData.url
        val dirPath = workInputData.path
        val fileName = workInputData.fileName
        val connectTimeOutInMs = workInputData.downloadConfig.connectTimeOutInMs
        val readTimeOutInMs = workInputData.downloadConfig.readTimeOutInMs
        val headers = workInputData.headers

        return try {

            val totalLength = DownloadTask(
                url = url,
                path = dirPath,
                fileName = fileName,
                downloadService = RetrofitInstance.getDownloadService(
                    connectTimeOutInMs,
                    readTimeOutInMs
                )
            ).download(
                headers = headers,
                onStart = {
                    setProgressAsync(
                        workDataOf(
                            DownloadConst.KEY_STATE to DownloadConst.STARTED,
                            DownloadConst.KEY_ID to id,
                            DownloadConst.KEY_LENGTH to it
                        )
                    )
                },
                onProgress = { progress, length, speed ->
                    setProgressAsync(
                        workDataOf(
                            DownloadConst.KEY_STATE to DownloadConst.PROGRESS,
                            DownloadConst.KEY_ID to id,
                            DownloadConst.KEY_PROGRESS to progress,
                            DownloadConst.KEY_LENGTH to length,
                            DownloadConst.KEY_SPEED to speed
                        )
                    )
                }
            )
            Result.success()
        } catch (e: Exception) {
            Result.failure(
                workDataOf(ExceptionConst.KEY_EXCEPTION to e.message)
            )
        }

    }

}

