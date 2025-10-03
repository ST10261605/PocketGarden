package com.example.pocketgarden.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

//plant.id api interface --> calling the identification endpoint with api key in header and
//including details of identified plant
interface PlantIdApi {
    @POST("identify")
    suspend fun identify(
        @Header("Api-Key") apiKey: String,
        @Query("details") details: String?,
        @Body request: IdentificationRequest
    ): Response<IdentificationResponse>

    companion object {
        private const val BASE_URL = "https://api.plant.id/v3/" // base URL

        fun create(): PlantIdApi {
            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()

            return retrofit.create(PlantIdApi::class.java)
        }
    }
}
