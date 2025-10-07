package com.example.pocketgarden.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

interface PlantIdApi {
    @POST("v2/identify")
    suspend fun identify(
        @Header("Api-Key") apiKey: String,
        @Body request: IdentificationRequestV3
    ): Response<IdentificationResponse>

    companion object {
        private const val BASE_URL = "https://api.plant.id/"

        fun create(): PlantIdApi {
            // Add logging interceptor to see full request/response
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()

            return retrofit.create(PlantIdApi::class.java)
        }
    }
}