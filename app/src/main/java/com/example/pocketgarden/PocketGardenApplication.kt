package com.example.pocketgarden

import android.app.Application
import com.example.pocketgarden.worker.FirestoreSyncWorker

class PocketGardenApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase (this happens automatically with google-services.json)
        // Schedule periodic sync
        scheduleFirestoreSync()
    }

    private fun scheduleFirestoreSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = androidx.work.PeriodicWorkRequestBuilder<FirestoreSyncWorker>(
            1, java.util.concurrent.TimeUnit.HOURS
        ).setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueue(syncWorkRequest)
    }
}