package com.example.myapplication.di

import com.example.myapplication.data.remote.FraudApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {
    // ─── IMPORTANT: Replace YOUR_LAPTOP_IP with your actual IP
    // To find it: Open CMD → type ipconfig → look for IPv4 Address
    // Example: http://192.168.1.105:5000/
    private const val BASE_URL = "10.158.124.140"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val fraudApiService: FraudApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(FraudApiService::class.java)
    }
}
