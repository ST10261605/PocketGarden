package com.example.pocketgarden.network

import com.google.gson.annotations.SerializedName

// Exact match to your working Postman request
data class IdentificationRequestV3(
    @SerializedName("images")
    val images: List<String>,

    @SerializedName("modifiers")
    val modifiers: List<String> = listOf("similar_images"),

    @SerializedName("organs")
    val organs: List<String> = listOf("leaf"),

    @SerializedName("latitude")
    val latitude: Double? = 0.0,

    @SerializedName("longitude")
    val longitude: Double? = 0.0,

    @SerializedName("lang")
    val lang: String = "en"
)

data class IdentificationResponse(
    @SerializedName("id")
    val id: Long?,

    @SerializedName("custom_id")
    val customId: String?,

    @SerializedName("meta_data")
    val metaData: MetaData?,

    @SerializedName("result")
    val result: Result?,

    @SerializedName("error")
    val error: String?,

    @SerializedName("suggestions")
    val suggestions: List<Suggestion>? // Direct suggestions field from v2 API
)

data class MetaData(
    @SerializedName("latitude")
    val latitude: Double?,

    @SerializedName("longitude")
    val longitude: Double?,

    @SerializedName("date")
    val date: String?,

    @SerializedName("datetime")
    val datetime: String?
)

data class Result(
    @SerializedName("is_plant")
    val isPlant: IsPlant?,

    @SerializedName("classification")
    val classification: Classification?,

    // Add these fields for v2 API compatibility
    @SerializedName("is_plant_probability")
    val isPlantProbability: Double?,

    @SerializedName("suggestions")
    val suggestions: List<Suggestion>?
)

data class IsPlant(
    @SerializedName("binary")
    val binary: Boolean?,

    @SerializedName("probability")
    val probability: Double?
)

data class Classification(
    @SerializedName("suggestions")
    val suggestions: List<Suggestion>?
)

data class Suggestion(
    @SerializedName("id")
    val id: Long?,

    @SerializedName("plant_name")
    val plantName: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("probability")
    val probability: Double?,

    @SerializedName("confirmed")
    val confirmed: Boolean?,

    @SerializedName("similar_images")
    val similarImages: List<SimilarImage>?,

    @SerializedName("plant_details")
    val plantDetails: PlantDetails?
)

data class SimilarImage(
    @SerializedName("id")
    val id: String?,

    @SerializedName("url")
    val url: String?,

    @SerializedName("license_name")
    val licenseName: String?,

    @SerializedName("license_url")
    val licenseUrl: String?,

    @SerializedName("similarity")
    val similarity: Double?,

    @SerializedName("url_small")
    val urlSmall: String?
)

data class PlantDetails(
    @SerializedName("common_names")
    val commonNames: List<String>?,

    @SerializedName("url")
    val url: String?,

    @SerializedName("description")
    val description: Map<String, String>?,

    @SerializedName("taxonomy")
    val taxonomy: Taxonomy?,

    @SerializedName("rank")
    val rank: String?,

    @SerializedName("gbif_id")
    val gbifId: Int?,

    @SerializedName("scientific_name")
    val scientificName: String?,

    @SerializedName("structured_name")
    val structuredName: StructuredName?
)

data class Taxonomy(
    @SerializedName("class")
    val className: String?,

    @SerializedName("family")
    val family: String?,

    @SerializedName("genus")
    val genus: String?,

    @SerializedName("kingdom")
    val kingdom: String?,

    @SerializedName("order")
    val order: String?,

    @SerializedName("phylum")
    val phylum: String?
)

data class StructuredName(
    @SerializedName("genus")
    val genus: String?,

    @SerializedName("species")
    val species: String?,

    @SerializedName("hybrid")
    val hybrid: String?
)