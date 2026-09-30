package com.example.gesticks.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    // Nota: Deberás reemplazar esto con la URL de tu API intermedia (Node.js, etc.)
    // que se conecta a MongoDB, ya que no se recomienda conectar directo desde Android.
    private const val BASE_URL = "https://backend-movil-4x6m.onrender.com/api/"

    // Mismo servidor, sin el prefijo /api/: el socket de notificaciones vive en la raíz.
    const val SOCKET_URL = "https://backend-movil-4x6m.onrender.com"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val api: GestixApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GestixApiService::class.java)
    }
}
