package com.example.pocketgarden

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pocketgarden.data.local.PlantDAO
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.data.local.PlantNote
import com.example.pocketgarden.data.local.PlantNoteDAO

@Database(entities = [User::class, PlantEntity::class, PlantNote::class] , version = 6)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDAO
    abstract fun plantDao(): PlantDAO

    abstract fun plantNoteDao(): PlantNoteDAO

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocketgarden_db"
                )
                    .fallbackToDestructiveMigration() // handles schema change -- when readying app for google play store make sure to remove this
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}