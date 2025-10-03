package com.example.pocketgarden.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private const val BASE_URL = "https://api.plant.id/v3/" // this is the base URL of the API

    //using retrofit as it allows us to connect with the plant.id REST API through HTTP requests
    //HTTP requests include GET, POST, DELETE, PUT, PATCH
    //it also converts API responses into Java/Kotlin objects
    //it also enables authentication and custom headers (which is needed as we have a custom api key for plant.id)

    //Code Reference:
    //GeeksForGeeks(2025),"Introduction to Retrofit in Android", Available At:(https://www.geeksforgeeks.org/android/introduction-retofit-2-android-set-1/)

    fun provideRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun providePlantIdApi(): PlantIdApi = provideRetrofit().create(PlantIdApi::class.java)
}