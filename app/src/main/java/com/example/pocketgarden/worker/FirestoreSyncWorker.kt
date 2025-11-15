package com.example.pocketgarden.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pocketgarden.repository.PlantRepository
import com.example.pocketgarden.AppDatabase

class FirestoreSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("FirestoreSyncWorker", "Starting Firestore sync...")

            val plantRepository = PlantRepository.getInstance(applicationContext)
            val syncResult = plantRepository.syncPendingPlants()

            when (syncResult) {
                is PlantRepository.SyncResult.SUCCESS -> {
                    Log.d("FirestoreSyncWorker", "Sync completed: ${syncResult.successCount} success, ${syncResult.failureCount} failures")
                    if (syncResult.failureCount == 0) Result.success()
                    else Result.retry()
                }
                is PlantRepository.SyncResult.ERROR -> {
                    Log.e("FirestoreSyncWorker", "Sync error: ${syncResult.message}")
                    Result.retry()
                }
                PlantRepository.SyncResult.NO_NETWORK -> {
                    Log.d("FirestoreSyncWorker", "No network available for sync")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncWorker", "Sync failed: ${e.message}", e)
            Result.retry()
        }
    }
}