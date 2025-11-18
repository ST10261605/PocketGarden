package com.example.pocketgarden.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.pocketgarden.AppDatabase
import com.example.pocketgarden.FirestoreSyncRepository
import com.example.pocketgarden.data.local.PlantDAO
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.data.local.PlantNote
import com.example.pocketgarden.data.local.PlantNoteDAO
import com.example.pocketgarden.data.local.PlantReminder
import com.example.pocketgarden.data.local.ReminderType
import com.example.pocketgarden.data.local.SyncStatus
import com.example.pocketgarden.network.PlantIdApi
import com.example.pocketgarden.network.IdentificationRequestV3
import com.example.pocketgarden.network.IdentificationResponse
import com.example.pocketgarden.network.NetworkHelper
import com.example.pocketgarden.notifications.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Calendar

class PlantRepository(
    private val api: PlantIdApi,
    private val plantDao: PlantDAO,
    val apiKeyProvider: ApiKeyProvider,
    private val firestoreSyncRepository: FirestoreSyncRepository,
    private val connectivityManager: ConnectivityManager,
    private val plantNoteDao: PlantNoteDAO,
    private val syncRepository: SyncRepository,
    private val plantReminderDao: com.example.pocketgarden.data.local.PlantReminderDAO, // Add this
    private val alarmScheduler: AlarmScheduler, // Add this
    private val context: Context // Add this for the alarm scheduler
) {

    sealed class SyncResult {
        object NO_NETWORK : SyncResult()
        data class SUCCESS(val successCount: Int, val failureCount: Int) : SyncResult()
        data class ERROR(val message: String) : SyncResult()
    }

    suspend fun addPlantOffline(imageUri: String): Long {
        val entity = PlantEntity(imageUri = imageUri, synced = false, status = "PENDING")
        return plantDao.insert(entity)
    }

    suspend fun savePlant(plant: PlantEntity): Long {
        val localId = plantDao.insert(plant)

        // Try to sync to Firestore immediately if online
        if (isOnline()) {
            syncPlantToFirestore(localId)
        }

        return localId
    }

    suspend fun deletePlant(plant: PlantEntity) {
        // If plant is synced to Firestore, mark for deletion
        if (plant.firestoreId != null) {
            val updatedPlant = plant.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = System.currentTimeMillis()
            )
            plantDao.update(updatedPlant)

            // Try to delete from Firestore immediately if online
            if (isOnline()) {
                syncDeletions()
            }
        } else {
            // If never synced, just delete locally
            plantDao.delete(plant)
        }
    }

    // Plant note functionality -- updated with offline sync feature
    suspend fun addPlantNote(note: PlantNote) {
        plantNoteDao.insert(note)
        // try to sync immediately if online
        try {
            syncRepository.syncNotes()
        } catch (e: Exception) {
            // Note will remain unsynced and will be synced later
            Log.d("PlantRepository", "Note saved offline, will sync later")
        }
    }

    fun getPlantNotes(plantLocalId: Long): Flow<List<PlantNote>> {
        return plantNoteDao.getNotesForPlant(plantLocalId)
    }

    // Delete plant note -- with offline sync
    suspend fun deletePlantNote(note: PlantNote) {
        plantNoteDao.delete(note)
        // try to delete from Firestore
        syncRepository.deleteNoteFromFirestore(note)
    }

    // Update plant note -- with offline sync
    suspend fun updatePlantNote(note: PlantNote) {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis(), isSynced = false)
        plantNoteDao.update(updatedNote)
        // Try to sync
        try {
            syncRepository.syncNotes()
        } catch (e: Exception) {
            Log.d("PlantRepository", "Note update saved offline, will sync later")
        }
    }

    // Function to get total number of notes for plants
    suspend fun getNoteCountForPlant(plantLocalId: Long): Int {
        return plantNoteDao.getNoteCountForPlant(plantLocalId)
    }

    // Function to sync all plant notes
    suspend fun syncAllNotes() {
        syncRepository.syncNotes()
    }

    suspend fun syncPendingPlants(): SyncResult {
        return try {
            if (!isOnline()) {
                return SyncResult.NO_NETWORK
            }

            val unsyncedPlants = plantDao.getUnsyncedPlants()
            var successCount = 0
            var failureCount = 0

            unsyncedPlants.forEach { plant ->
                // Update sync status to SYNCING
                plantDao.updateSyncStatus(plant.localId, SyncStatus.SYNCING, System.currentTimeMillis())

                val syncSuccess = firestoreSyncRepository.syncPlantToFirestore(plant)

                if (syncSuccess) {
                    // For Firestore, need to handle the document ID
                    // might need to fetch the actual ID
                    val updatedPlant = plant.copy(
                        syncStatus = SyncStatus.SYNCED,
                        firestoreId = plant.firestoreId ?: "firestore_${plant.localId}",
                        updatedAt = System.currentTimeMillis()
                    )
                    plantDao.update(updatedPlant)
                    successCount++
                } else {
                    plantDao.updateSyncStatusWithError(
                        plant.localId,
                        SyncStatus.FAILED,
                        "Sync failed",
                        System.currentTimeMillis()
                    )
                    failureCount++
                }
            }

            // Sync deletions
            syncDeletions()

            SyncResult.SUCCESS(successCount, failureCount)
        } catch (e: Exception) {
            Log.e("PlantRepository", "Error syncing pending plants: ${e.message}")
            SyncResult.ERROR(e.message ?: "Unknown error")
        }
    }

    private suspend fun syncPlantToFirestore(localId: Long) {
        val plant = plantDao.getPlantById(localId) ?: return
        plantDao.updateSyncStatus(localId, SyncStatus.SYNCING, System.currentTimeMillis())

        val syncSuccess = firestoreSyncRepository.syncPlantToFirestore(plant)

        if (syncSuccess) {
            val updatedPlant = plant.copy(
                syncStatus = SyncStatus.SYNCED,
                firestoreId = plant.firestoreId ?: "firestore_${plant.localId}",
                updatedAt = System.currentTimeMillis()
            )
            plantDao.update(updatedPlant)
        } else {
            plantDao.updateSyncStatusWithError(
                localId,
                SyncStatus.FAILED,
                "Sync failed",
                System.currentTimeMillis()
            )
        }
    }

    private suspend fun syncDeletions() {
        // Get plants marked for deletion and sync them
        val plantsToDelete = plantDao.getPlantsMarkedForDeletion()
        plantsToDelete.forEach { plant ->
            plant.firestoreId?.let { firestoreId ->
                val deleteSuccess = firestoreSyncRepository.deletePlantFromFirestore(firestoreId)
                if (deleteSuccess) {
                    plantDao.delete(plant)
                }
            }
        }
    }

    // Reminder methods
    suspend fun setWaterReminder(plant: PlantEntity, frequencyDays: Int, timeOfDay: String) {
        // Parse timeOfDay (e.g., "09:00") and calculate next reminder time
        val calendar = Calendar.getInstance().apply {
            val timeParts = timeOfDay.split(":").map { it.toInt() }
            val hour = timeParts[0]
            val minute = timeParts[1]
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val reminder = PlantReminder(
            plantLocalId = plant.localId,
            plantName = plant.name,
            reminderType = ReminderType.WATERING,
            reminderTime = calendar.timeInMillis,
            repeatInterval = frequencyDays * 24 * 60 * 60 * 1000L // Convert days to milliseconds
        )

        plantReminderDao.insert(reminder)
        alarmScheduler.scheduleReminder(reminder)

        // Update plant entity
        val updatedPlant = plant.copy(
            wateringFrequency = frequencyDays,
            waterReminderEnabled = true,
            nextWatering = calendar.timeInMillis,
            updatedAt = System.currentTimeMillis()
        )
        plantDao.update(updatedPlant)
    }

    suspend fun cancelWaterReminder(plant: PlantEntity) {
        val reminder = plantReminderDao.getReminderForPlant(plant.localId, ReminderType.WATERING)
        reminder?.let {
            alarmScheduler.cancelReminder(it.id)
            plantReminderDao.delete(it)
        }

        // Update plant entity
        val updatedPlant = plant.copy(
            waterReminderEnabled = false,
            nextWatering = null,
            updatedAt = System.currentTimeMillis()
        )
        plantDao.update(updatedPlant)
    }

    suspend fun markPlantAsWatered(plant: PlantEntity) {
        val now = System.currentTimeMillis()
        val updatedPlant = plant.copy(
            lastWateredAt = now,
            nextWatering = now + (plant.wateringFrequency * 24 * 60 * 60 * 1000L),
            updatedAt = now
        )
        plantDao.update(updatedPlant)

        // Reschedule the reminder
        val reminder = plantReminderDao.getReminderForPlant(plant.localId, ReminderType.WATERING)
        reminder?.let {
            val newReminder = it.copy(
                reminderTime = updatedPlant.nextWatering ?: (now + plant.wateringFrequency * 24 * 60 * 60 * 1000L),
                updatedAt = now
            )
            plantReminderDao.update(newReminder)
            alarmScheduler.rescheduleReminder(newReminder)
        }
    }

    suspend fun getRemindersForPlant(plantLocalId: Long): Flow<List<PlantReminder>> {
        return plantReminderDao.getRemindersForPlant(plantLocalId)
    }

    suspend fun rescheduleAllReminders() {
        val activeReminders = plantReminderDao.getActiveReminders()
        activeReminders.forEach { reminder ->
            alarmScheduler.scheduleReminder(reminder)
        }
    }

    private fun isOnline(): Boolean {
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    suspend fun getAllPlantsFlow() = plantDao.getAllPlants()

    companion object {
        @Volatile private var INSTANCE: PlantRepository? = null

        fun getInstance(context: Context): PlantRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val dao = db.plantDao()
                val api = PlantIdApi.create()
                val plantNoteDao = db.plantNoteDao()
                val plantReminderDao = db.plantReminderDao() // Add this

                // Get ConnectivityManager
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                // Create FirestoreSyncRepository
                val firestoreSyncRepository = FirestoreSyncRepository().apply {
                    enableOfflinePersistence()
                }

                // Create NetworkHelper
                val networkHelper = NetworkHelper(context)

                // Create AlarmScheduler
                val alarmScheduler = AlarmScheduler(context) // Add this

                // Create SyncRepository
                val syncRepository = SyncRepository(
                    plantNoteDao = plantNoteDao,
                    plantDao = dao,
                    networkHelper = networkHelper,
                    context = context
                )

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

                PlantRepository(
                    api = api,
                    plantDao = dao,
                    apiKeyProvider = provider,
                    firestoreSyncRepository = firestoreSyncRepository,
                    connectivityManager = connectivityManager,
                    plantNoteDao = plantNoteDao,
                    syncRepository = syncRepository,
                    plantReminderDao = plantReminderDao, // Add this
                    alarmScheduler = alarmScheduler, // Add this
                    context = context // Add this
                ).also { INSTANCE = it }
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

sealed class IdentificationResult {
    data class Success(val response: IdentificationResponse?): IdentificationResult()
    data class Error(val code: Int, val message: String): IdentificationResult()
}

// Small interface to provide key & helper to read URIs (so repository stays testable)
interface ApiKeyProvider {
    fun getApiKey(): String
    suspend fun readUriAsBase64(uriString: String): String
}