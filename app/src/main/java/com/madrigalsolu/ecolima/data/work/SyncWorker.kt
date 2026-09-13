package com.madrigalsolu.ecolima.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.madrigalsolu.ecolima.data.repository.RegistroRepository

/**
 * Background sync worker for offline-first.
 *
 * - Runs when network is available (NetworkType.CONNECTED).
 * - Calls [RegistroRepository.syncPending] to push PENDIENTE rows.
 * - Retry strategy: WorkManager backoff; repository keeps rows PENDIENTE on network error.
 *
 * ### SQLite + REST advantage (KDoc for deliverable)
 * SQLite provides instant persistence even without connectivity; WorkManager
 * guarantees eventual sync when connectivity returns, without blocking UI.
 * This decouples perceived performance (local write < 50ms) from network latency.
 */
/**
 * For Hilt projects, use @HiltWorker + @AssistedInject to inject [RegistroRepository].
 * For non-Hilt projects, inject via custom WorkerFactory (see [EcolimWorkerFactory] below).
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val repository: RegistroRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = repository.syncPending()
            if (result.failed > 0 && result.synced == 0) {
                // All failed due to network: retry with backoff
                Result.retry()
            } else if (result.failed > 0) {
                // Partial success: consider success but log errors
                Result.success()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "ecolim_sync_worker"

        /**
         * Enqueue a one-time sync with network constraint.
         * Call from Repository after insert, or from UI/NetworkCallback.
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag(UNIQUE_WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }
}

/**
 * Custom WorkerFactory for non-Hilt projects.
 * Register in Application.onCreate():
 *   WorkManager.initialize(context, Configuration.Builder()
 *     .setWorkerFactory(EcolimWorkerFactory(repository)).build(), ...)
 */
class EcolimWorkerFactory(
    private val repository: RegistroRepository
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): androidx.work.ListenableWorker? {
        return when (workerClassName) {
            SyncWorker::class.java.name ->
                SyncWorker(appContext, workerParameters, repository)
            else -> null
        }
    }
}
