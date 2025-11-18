package com.example.pocketgarden

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.pocketgarden.network.NetworkHelper
import com.example.pocketgarden.repository.PlantRepository
import kotlinx.coroutines.launch

class PocketGardenApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupPeriodicSync()
    }

    private fun setupPeriodicSync() {
        val networkHelper = NetworkHelper(this)

        // Sync when app comes to foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                if (networkHelper.isOnline()) {
                    owner.lifecycleScope.launch {
                        val plantRepository = PlantRepository.getInstance(this@PocketGardenApplication)
                        plantRepository.syncAllNotes()
                    }
                }
            }
        })
    }
}