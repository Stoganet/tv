package com.stoganet.tv.data.net

import com.stoganet.tv.BuildConfig
import com.stoganet.tv.api.model.RefreshRequest
import com.stoganet.tv.api.model.TokenPair
import com.stoganet.tv.data.auth.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

const val BASE_URL = "https://api.stoganet.com/"

fun buildHttpClient(tokenStore: TokenStore): HttpClient = HttpClient(OkHttp) {
    configurePlugins(tokenStore, BASE_URL)
    engine {
        config {
            connectTimeout(10, TimeUnit.SECONDS)
            readTimeout(15, TimeUnit.SECONDS)
            writeTimeout(15, TimeUnit.SECONDS)
        }
    }
}

internal fun HttpClientConfig<*>.configurePlugins(tokenStore: TokenStore, baseUrl: String) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        })
    }
    install(Auth) {
        bearer {
            sendWithoutRequest { true }
            loadTokens {
                val access = tokenStore.accessToken() ?: return@loadTokens null
                val refresh = tokenStore.refreshToken() ?: return@loadTokens null
                BearerTokens(access, refresh)
            }
            refreshTokens {
                val refreshToken = oldTokens?.refreshToken ?: return@refreshTokens null
                val response = this.client.post("${baseUrl}auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequest(refreshToken = refreshToken))
                    markAsRefreshTokenRequest()
                }
                if (!response.status.isSuccess()) {
                    tokenStore.clear()
                    return@refreshTokens null
                }
                val pair = response.body<TokenPair>()
                tokenStore.saveTokens(pair)
                BearerTokens(pair.accessToken, pair.refreshToken)
            }
        }
    }
    if (BuildConfig.DEBUG) {
        install(Logging) { level = LogLevel.INFO }
    }
}
