package com.stoganet.tv.data.net

import com.stoganet.tv.api.model.LoginRequest
import com.stoganet.tv.api.model.QuickConnectPollRequest
import com.stoganet.tv.api.model.QuickConnectStartResponse
import com.stoganet.tv.api.model.RefreshRequest
import com.stoganet.tv.api.model.TokenPair
import com.stoganet.tv.data.auth.QuickConnectPollResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class StoganetApi(private val client: HttpClient, private val baseUrl: String = BASE_URL) {

    suspend fun login(username: String, password: String, deviceLabel: String?): TokenPair {
        val response = client.post("${baseUrl}auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = username, password = password, deviceLabel = deviceLabel))
        }
        check(response.status.isSuccess()) { "login failed: ${response.status.value}" }
        return response.body()
    }

    suspend fun startQuickConnect(): QuickConnectStartResponse {
        val response = client.post("${baseUrl}auth/quick-connect/start") {
            contentType(ContentType.Application.Json)
        }
        check(response.status.isSuccess()) { "startQuickConnect failed: ${response.status.value}" }
        return response.body()
    }

    suspend fun pollQuickConnect(pollToken: String): QuickConnectPollResult {
        val response = client.post("${baseUrl}auth/quick-connect/poll") {
            contentType(ContentType.Application.Json)
            setBody(QuickConnectPollRequest(pollToken = pollToken))
        }
        return when (response.status.value) {
            200 -> QuickConnectPollResult.Success(response.body())
            202 -> QuickConnectPollResult.Pending
            410 -> QuickConnectPollResult.Expired
            else -> error("Unexpected poll status: ${response.status.value}")
        }
    }

    suspend fun logout(refreshToken: String) {
        val response = client.post("${baseUrl}auth/logout") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken = refreshToken))
        }
        check(response.status.isSuccess()) { "logout failed: ${response.status.value}" }
    }

    suspend fun logoutAll() {
        val response = client.post("${baseUrl}auth/logout/all")
        check(response.status.isSuccess()) { "logoutAll failed: ${response.status.value}" }
    }
}
