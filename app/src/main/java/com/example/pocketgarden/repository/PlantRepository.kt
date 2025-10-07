package com.example.pocketgarden.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.pocketgarden.AppDatabase
import com.example.pocketgarden.data.local.PlantDAO
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.network.PlantIdApi
import com.example.pocketgarden.network.IdentificationRequestV3
import com.example.pocketgarden.network.IdentificationResponse // Add this import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
                                Log.d("ApiKeyProvider", "Processing URI: $uriString")

                                val uri = Uri.parse(uriString)
                                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

                                if (inputStream == null) {
                                    Log.e("ApiKeyProvider", "Could not open input stream for URI: $uriString")
                                    return@withContext ""
                                }

                                // Read the raw bytes
                                val rawBytes = inputStream.readBytes()
                                inputStream.close()

                                // Convert to pure Base64 without data URL prefix
                                val base64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                                Log.d("ApiKeyProvider", "Pure base64 length: ${base64.length}")

                                return@withContext base64

                            } catch (e: Exception) {
                                Log.e("ApiKeyProvider", "Error reading URI as Base64: ${e.message}", e)
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
                Log.d("PlantRepository", "Starting plant identification...")
                Log.d("PlantRepository", "Base64 first 50 chars: ${base64.take(50)}")
                Log.d("PlantRepository", "Base64 length: ${base64.length}")

                // Build the request
                val request = IdentificationRequestV3(
                    images = listOf(base64),
                    modifiers = listOf("similar_images"),
                    organs = listOf("leaf"),
                    latitude = 0.0,
                    longitude = 0.0,
                    lang = "en"
                )

                // Call the Plant.id API
                val resp = api.identify(
                    apiKey = apiKeyProvider.getApiKey(),
                    request = request
                )

                Log.d("PlantRepository", "Response code: ${resp.code()}")
                Log.d("PlantRepository", "Response isSuccessful: ${resp.isSuccessful}")

                if (resp.isSuccessful) {
                    val responseBody = resp.body()
                    Log.d("PlantRepository", "Full response: $responseBody")

                    // Debug: Log the raw response to see actual structure
                    val rawResponse = resp.raw().toString()
                    Log.d("PlantRepository", "Raw response: $rawResponse")

                    // Handle different response structures
                    val suggestions = when {
                        // v3 structure: result -> classification -> suggestions
                        responseBody?.result?.classification?.suggestions != null -> {
                            Log.d("PlantRepository", "Using v3 structure")
                            responseBody.result.classification.suggestions
                        }
                        // v2 structure: direct suggestions field
                        responseBody?.suggestions != null -> {
                            Log.d("PlantRepository", "Using v2 structure")
                            responseBody.suggestions
                        }
                        // v3 alternative: result -> suggestions
                        responseBody?.result?.suggestions != null -> {
                            Log.d("PlantRepository", "Using v3 alternative structure")
                            responseBody.result.suggestions
                        }
                        else -> {
                            Log.d("PlantRepository", "No suggestions found in any structure")
                            emptyList()
                        }
                    }

                    Log.d("PlantRepository", "Found ${suggestions.size} suggestions")

                    if (suggestions.isEmpty()) {
                        Log.d("PlantRepository", "No plant suggestions found")
                        return@withContext IdentificationResult.Success(responseBody)
                    }

                    IdentificationResult.Success(responseBody)
                } else {
                    val errorBody = resp.errorBody()?.string()
                    Log.e("PlantRepository", "API Error: ${resp.code()} - $errorBody")
                    IdentificationResult.Error(resp.code(), errorBody ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e("PlantRepository", "Exception during identification: ${e.message}", e)
                IdentificationResult.Error(-1, e.localizedMessage ?: "Exception occurred")
            }
        }
    }
    }

//    // Worker will call this to sync pending items:
//    suspend fun syncPendingIdentifications(): List<Pair<Long, Boolean>> {
//        val pending = plantDao.getPendingPlants()
//        val results = mutableListOf<Pair<Long, Boolean>>()
//        pending.forEach { plant ->
//            try {
//                // load file bytes from URI
//                val base64 = apiKeyProvider.readUriAsBase64(plant.imageUri)
//                val req = IdentificationRequestV3(images = listOf(base64))
//                val resp = api.identify(apiKeyProvider.getApiKey(), req)
//                if (resp.isSuccessful) {
//                    val body = resp.body()
//                    // choose top suggestion (if any)
//                    val suggestion = body?.result?.classification?.suggestions?.firstOrNull()
//                    val name = suggestion?.name ?: "Unknown Plant" // Fixed: provide default name
//                    val updated = plant.copy(
//                        remoteId = body?.id ?: "", // Fixed: provide default for id
//                        name = name,
//                        synced = true,
//                        status = "IDENTIFIED"
//                    )
//                    plantDao.update(updated)
//                    results.add(plant.localId to true)
//                } else {
//                    // if rate limited, throw or mark as failed
//                    val updated = plant.copy(status = "FAILED")
//                    plantDao.update(updated)
//                    results.add(plant.localId to false)
//                }
//            } catch (ex: Exception) {
//                val updated = plant.copy(status = "FAILED")
//                plantDao.update(updated)
//                results.add(plant.localId to false)
//            }
//        }
//        return results
//    }
//
//    suspend fun getAllPlantsFlow() = plantDao.getAllPlants()
//}

sealed class IdentificationResult {
    data class Success(val response: IdentificationResponse?): IdentificationResult() // Fixed: removed package prefix
    data class Error(val code: Int, val message: String): IdentificationResult()
}

// small interface to provide key & helper to read URIs (so repository stays testable)
interface ApiKeyProvider {
    fun getApiKey(): String
    suspend fun readUriAsBase64(uriString: String): String
}