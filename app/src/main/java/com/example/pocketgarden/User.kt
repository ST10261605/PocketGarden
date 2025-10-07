package com.example.pocketgarden

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User (
    @PrimaryKey(autoGenerate = true) val id: Int=0,
    val email: String,
    val password: String,
    val profileImageUri: String? = null,
    val fingerprintEnabled: Boolean = false
)