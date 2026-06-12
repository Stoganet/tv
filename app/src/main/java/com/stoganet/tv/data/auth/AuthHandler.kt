package com.stoganet.tv.data.auth

import com.stoganet.tv.api.DefaultApi
import com.stoganet.tv.api.model.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

class AuthHandler(private val tokenStore: TokenStore, private val api: DefaultApi) :
    Interceptor,
    Authenticator {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.accessToken() }
        return chain.proceed(chain.request().withBearer(token))
    }

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        val tokenAtRequest = response.request.header("Authorization")?.removePrefix("Bearer ")
        val currentToken = runBlocking { tokenStore.accessToken() }

        if (currentToken != null && currentToken != tokenAtRequest) {
            return response.request.withBearer(currentToken)
        }

        val refreshToken = runBlocking { tokenStore.refreshToken() } ?: return null

        val newAccessToken = runBlocking {
            try {
                val refreshResponse = api.postAuthRefresh(RefreshRequest(refreshToken))
                if (!refreshResponse.isSuccessful) {
                    tokenStore.clear()
                    return@runBlocking null
                }
                val pair = refreshResponse.body() ?: return@runBlocking null
                tokenStore.saveTokens(pair)
                pair.accessToken
            } catch (_: IOException) {
                null
            }
        }

        return newAccessToken?.let { response.request.withBearer(it) }
    }
}

private fun Request.withBearer(token: String?): Request =
    if (token != null) newBuilder().header("Authorization", "Bearer $token").build() else this
