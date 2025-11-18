package com.example.pocketgarden.data.local

data class FirestorePlantNote(
    val id: String = "",
    val plantFirestoreId: String = "", // need to store this in PlantEntity
    val content: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val userId: String = "" // user authentication
) {
    constructor() : this("", "", "", 0L, 0L, "")
}