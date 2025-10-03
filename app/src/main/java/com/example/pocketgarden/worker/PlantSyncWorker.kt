package com.example.pocketgarden.worker

import android.content.Context
import androidx.room.Database
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pocketgarden.AppDatabase
import com.example.pocketgarden.BuildConfig
import com.example.pocketgarden.network.NetworkModule
import com.example.pocketgarden.repository.ApiKeyProvider
import com.example.pocketgarden.repository.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//this class is a CoroutineWorker to sync pending identifications
class PlantSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db = AppDatabase.getDatabase(appContext)
    private val plantDao = db.plantDao()
    private val api = NetworkModule.providePlantIdApi()
    private val apiKeyProvider = object : ApiKeyProvider {
        override fun getApiKey(): String {

            return BuildConfig.PLANT_ID_API_KEY
        }

        override suspend fun readUriAsBase64(uriString: String): String {
            return withContext(Dispatchers.IO) {
                val uri = android.net.Uri.parse(uriString)
                appContext.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                } ?: ""
            }
        }
    }
    private val repo = PlantRepository(api, plantDao, apiKeyProvider)

    override suspend fun doWork(): Result {
        return try {
            val results = repo.syncPendingIdentifications("common_names,taxonomy,watering,toxicity")
            // determine outcome: if any failed, request retry
            val anyFailed = results.any { !it.second }
            if (anyFailed) Result.retry() else Result.success()
        } catch (ex: Exception) {
            Result.retry()
        }
    }
}