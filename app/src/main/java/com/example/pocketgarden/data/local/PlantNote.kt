package com.example.pocketgarden.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "plant_notes")
data class PlantNote(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val plantLocalId: Long, // reference to the plant
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)