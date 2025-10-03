package com.example.pocketgarden.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.pocketgarden.AppDatabase
import com.example.pocketgarden.data.local.PlantDAO
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.network.PlantIdApi
import com.example.pocketgarden.network.IdentificationRequestV3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class PlantRepository(
    private val api: PlantIdApi,
    private val plantDao: PlantDAO,
    val apiKeyProvider: ApiKeyProvider // interface to get API key or proxy URL
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
                        return withContext(Dispatchers.IO) {
                            try {
                                val uri = Uri.parse(uriString)
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val bytes = inputStream?.readBytes() ?: ByteArray(0)
                                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                ""
                            }
                        }
                    }
                }

                PlantRepository(api, dao, provider).also { INSTANCE = it }
            }
        }
    }

    suspend fun identifyPlantFromBitmapBase64V3(base64: String): IdentificationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Build the v3 request
                val request = IdentificationRequestV3(
                    images = listOf(base64),
                    modifiers = listOf("similar_images"),
                    organs = listOf("leaf") // optional: you can add "flower", "fruit" etc.
                )

                // Call the Plant.id v3 API
                val resp = api.identify(
                    apiKey = apiKeyProvider.getApiKey(),
                    details = null,
                    request = request    // matches parameter type in PlantIdApi
                )

                if (resp.isSuccessful) {
                    // Success! Return the body
                    IdentificationResult.Success(resp.body())
                } else {
                    // API returned error
                    val errorMsg = resp.errorBody()?.string() ?: "Unknown error"
                    IdentificationResult.Error(resp.code(), errorMsg)
                }
            } catch (e: Exception) {
                // Network or other exception
                IdentificationResult.Error(-1, e.localizedMessage ?: "Exception occurred")
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
                val req = IdentificationRequestV3(images = listOf(base64))
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
