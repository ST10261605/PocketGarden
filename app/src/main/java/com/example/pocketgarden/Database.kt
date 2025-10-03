package com.example.pocketgarden

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pocketgarden.data.local.PlantDAO
import com.example.pocketgarden.data.local.PlantEntity

@Database(entities = [User::class, PlantEntity::class] , version = 2)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDAO
    abstract fun plantDao(): PlantDAO

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocketgarden_db"
                )
                    .fallbackToDestructiveMigration() // handles schema change
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}