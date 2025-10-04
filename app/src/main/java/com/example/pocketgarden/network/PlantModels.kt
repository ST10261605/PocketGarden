package com.example.pocketgarden.network

import com.google.gson.annotations.SerializedName

// request the identification endpoint, post with images, latitude, longitude, date&time images were taken
data class IdentificationRequestV3(
    val images: List<String>,
    val organs: List<String> = listOf("leaf"),       // optional: leaf, flower, fruit, stem
    val include_related_images: Boolean = true,      // get related images
    val modifiers: List<String>? = listOf("similar_images"), // optional
    val latitude: Double? = null,
    val longitude: Double? = null,
    val datetime: String? = null,
    val lang: String = "en"
)


// Response (trimmed to only the fields we use)
data class IdentificationResponse(
    val id: String?,
    val result: Result?
)

//using the probability here that the plant is likely a plant, using a boolean value for that
data class Result(
    @SerializedName("is_plant") val isPlant: BinaryProbability?,
    val classification: Classification?
)

data class BinaryProbability(
    val binary: Boolean?,
    val probability: Double?
)

//classification of plant e.g Vegetable
data class Classification(
    val suggestions: List<Suggestion>?
)


//suggestion of what plant it could be - id, name, probability that it is plant, details and common names
data class Suggestion(
    val id: Int?,
    val plant_name: String? = null,
    val name: String? = null, // some versions use `name`
    val probability: Double = 0.0,
    val details: Map<String, Any>? = null,
    val common_names: List<String>? = null
)