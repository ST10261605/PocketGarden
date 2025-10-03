package com.example.pocketgarden.repository

import android.content.Context
import android.util.Base64
import com.example.pocketgarden.AppDatabase
import com.example.pocketgarden.data.local.PlantDAO
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.network.PlantIdApi
import com.example.pocketgarden.network.IdentificationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class PlantRepository(
    private val api: PlantIdApi,
    private val plantDao: PlantDAO,
    private val apiKeyProvider: ApiKeyProvider // interface to get API key or proxy URL
) {

    //function to add a plant offline, the pending images are only sent for identification once user is back online
    //adding plant to roomdb until user returns online
    suspend fun addPlantOffline(imageUri: String): Long {
        val entity = PlantEntity(imageUri = imageUri, synced = false, status = "PENDING")
        return plantDao.insert(entity)
    }

    suspend fun savePlant(plant: PlantEntity) {
        plantDao.insert(plant)
    }

    companion object {
        @Volatile private var INSTANCE: PlantRepository? = null

        fun getInstance(context: Context): PlantRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val dao = db.plantDao()
                val api = PlantIdApi.create()
                val provider = object : ApiKeyProvider {
                    override fun getApiKey(): String = "mRnpO239bpQY3EcOGlxTgQ9GfXl2Krg6Xqqg4WhDkzzXEwSvlX"
                    override suspend fun readUriAsBase64(uriString: String): String {
                        // implement reading file bytes + Base64.encodeToString
                        return ""
                    }
                }
                PlantRepository(api, dao, provider).also { INSTANCE = it }
            }
        }
    }

    suspend fun identifyPlantFromBitmapBase64(base64: String, details: String?): IdentificationResult {
        // Directly calling Plant.id using API key from provider
        val key = apiKeyProvider.getApiKey()
        return withContext(Dispatchers.IO) {
            val request = IdentificationRequest(images = listOf(base64))
            val resp = api.identify(key, details, request)
            if (resp.isSuccessful) {
                val body = resp.body()
                IdentificationResult.Success(body)
            } else {
                IdentificationResult.Error(resp.code(), resp.message())
            }
        }
    }

    // Worker will call this to sync pending items:
    suspend fun syncPendingIdentifications(details: String?): List<Pair<Long, Boolean>> {
        val pending = plantDao.getPendingPlants()
        val results = mutableListOf<Pair<Long, Boolean>>()
        pending.forEach { plant ->
            try {
                // load file bytes from URI
                val base64 = apiKeyProvider.readUriAsBase64(plant.imageUri)
                val req = IdentificationRequest(images = listOf(base64))
                val resp = api.identify(apiKeyProvider.getApiKey(), details, req)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    // choose top suggestion (if any)
                    val suggestion = body?.result?.classification?.suggestions?.firstOrNull()
                    val name = suggestion?.plant_name ?: suggestion?.name ?: plant.name
                    val updated = plant.copy(
                        remoteId = body?.id,
                        name = name,
                        synced = true,
                        status = "IDENTIFIED"
                    )
                    plantDao.update(updated)
                    results.add(plant.localId to true)
                } else {
                    // if rate limited, throw or mark as failed
                    val updated = plant.copy(status = "FAILED")
                    plantDao.update(updated)
                    results.add(plant.localId to false)
                }
            } catch (ex: Exception) {
                val updated = plant.copy(status = "FAILED")
                plantDao.update(updated)
                results.add(plant.localId to false)
            }
        }
        return results
    }

    suspend fun getAllPlantsFlow() = plantDao.getAllPlants()
}



sealed class IdentificationResult {
    data class Success(val response: com.example.pocketgarden.network.IdentificationResponse?): IdentificationResult()
    data class Error(val code: Int, val message: String): IdentificationResult()
}

// small interface to provide key & helper to read URIs (so repository stays testable)
interface ApiKeyProvider {
    fun getApiKey(): String
    suspend fun readUriAsBase64(uriString: String): String
}
