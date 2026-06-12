package com.stoganet.tv.data.auth

import com.stoganet.tv.api.DefaultApi
import com.stoganet.tv.api.model.QuickConnectPollRequest
import com.stoganet.tv.api.model.QuickConnectStartResponse
import com.stoganet.tv.api.model.RefreshRequest
import com.stoganet.tv.api.model.TokenPair
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

// Retrofit only skips body conversion for 204/205; a 202 with empty body would throw
// JsonDecodingException when parsed as TokenPair. Use ResponseBody and parse manually.
private interface RawPollApi {
    @POST("auth/quick-connect/poll")
    suspend fun poll(@Body request: QuickConnectPollRequest): Response<ResponseBody>
}

class AuthRepository(private val api: DefaultApi, retrofit: Retrofit, private val tokenStore: TokenStore) {

    private val rawPollApi: RawPollApi = retrofit.create(RawPollApi::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun startQuickConnect(): Result<QuickConnectStartResponse> = runCatching {
        val response = api.postAuthQuickConnectStart()
        check(response.isSuccessful) { "server error: ${response.code()}" }
        response.body()!!
    }

    suspend fun pollQuickConnect(pollToken: String): QuickConnectPollResult {
        val response = rawPollApi.poll(QuickConnectPollRequest(pollToken))
        return when (response.code()) {
            HTTP_OK -> {
                val body = response.body() ?: throw IllegalStateException("200 poll had no body")
                QuickConnectPollResult.Success(json.decodeFromString(TokenPair.serializer(), body.string()))
            }

            HTTP_ACCEPTED -> QuickConnectPollResult.Pending

            HTTP_GONE -> QuickConnectPollResult.Expired

            else -> throw IllegalStateException("Unexpected poll response: ${response.code()}")
        }
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_ACCEPTED = 202
        const val HTTP_GONE = 410
    }

    suspend fun logout(refreshToken: String): Result<Unit> = runCatching {
        val response = api.postAuthLogout(RefreshRequest(refreshToken))
        check(response.isSuccessful) { "logout failed: ${response.code()}" }
        tokenStore.clear()
    }
}

sealed interface QuickConnectPollResult {
    data class Success(val tokens: TokenPair) : QuickConnectPollResult
    data object Pending : QuickConnectPollResult
    data object Expired : QuickConnectPollResult
}
