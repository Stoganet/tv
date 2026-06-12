package com.stoganet.tv.di

import android.content.Context
import com.stoganet.tv.api.DefaultApi
import com.stoganet.tv.data.auth.AuthHandler
import com.stoganet.tv.data.auth.AuthRepository
import com.stoganet.tv.data.auth.TokenStore
import com.stoganet.tv.data.net.HttpClients
import retrofit2.Retrofit

class ServiceLocator(context: Context) {

    private val appContext: Context = context.applicationContext

    val tokenStore: TokenStore by lazy { TokenStore.create(appContext) }

    private val rawRetrofit: Retrofit by lazy { HttpClients.retrofit(HttpClients.rawOkHttp) }

    private val api: DefaultApi by lazy { rawRetrofit.create(DefaultApi::class.java) }

    private val authHandler: AuthHandler by lazy { AuthHandler(tokenStore, api) }

    val authRepository: AuthRepository by lazy { AuthRepository(api, rawRetrofit) }

    val authedRetrofit: Retrofit by lazy {
        HttpClients.retrofit(HttpClients.authedOkHttp(authHandler, authHandler))
    }
}
