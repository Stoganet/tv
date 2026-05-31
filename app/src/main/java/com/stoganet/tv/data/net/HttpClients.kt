package com.stoganet.tv.data.net

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.stoganet.tv.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object HttpClients {

    private const val BASE_URL = "https://api.stoganet.com/"
    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val IO_TIMEOUT_SECONDS = 15L

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun authedOkHttp(authInterceptor: Interceptor): OkHttpClient =
        baseBuilder().addInterceptor(authInterceptor).build()

    fun rawOkHttp(): OkHttpClient = baseBuilder().build()

    fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private fun baseBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
                )
            }
        }
}
