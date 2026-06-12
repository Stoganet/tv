package com.stoganet.tv.data.auth

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.stoganet.tv.api.DefaultApi
import com.stoganet.tv.api.model.TokenPair
import com.stoganet.tv.api.model.User
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class AuthHandlerTest {

    private val server = MockWebServer()
    private lateinit var tokenStore: TokenStore
    private lateinit var authHandler: AuthHandler
    private lateinit var client: OkHttpClient

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        server.start()
        tokenStore = TokenStore(FakeDataStore())
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DefaultApi::class.java)
        authHandler = AuthHandler(tokenStore, api)
        client = OkHttpClient.Builder()
            .addInterceptor(authHandler)
            .authenticator(authHandler)
            .build()
    }

    @AfterEach
    fun tearDown() = server.close()

    @Test
    fun `request includes Bearer header when token stored`() = runTest {
        tokenStore.saveTokens(tokenPair("access-abc"))
        server.enqueue(MockResponse(code = 200))

        client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertEquals("Bearer access-abc", recorded.headers["Authorization"])
    }

    @Test
    fun `request has no Authorization header when no token stored`() = runTest {
        server.enqueue(MockResponse(code = 200))

        client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun `401 response triggers refresh and retries with new token`() = runTest {
        tokenStore.saveTokens(tokenPair("old-access"))
        server.enqueue(MockResponse(code = 401))
        server.enqueue(MockResponse(code = 200, body = tokenPairJson("new-access", "new-refresh")))
        server.enqueue(MockResponse(code = 200))

        client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val firstRequest = server.takeRequest()
        assertEquals("Bearer old-access", firstRequest.headers["Authorization"])

        server.takeRequest() // refresh call

        val retryRequest = server.takeRequest()
        assertEquals("Bearer new-access", retryRequest.headers["Authorization"])
        assertEquals("new-access", tokenStore.accessToken())
        assertEquals("new-refresh", tokenStore.refreshToken())
    }

    @Test
    fun `401 on refresh clears token store`() = runTest {
        tokenStore.saveTokens(tokenPair("old-access"))
        server.enqueue(MockResponse(code = 401))
        server.enqueue(MockResponse(code = 401))

        client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        assertNull(tokenStore.accessToken())
        assertNull(tokenStore.refreshToken())
    }

    private fun tokenPair(access: String, refresh: String = "refresh-default") = TokenPair(
        accessToken = access,
        refreshToken = refresh,
        user = User(id = "u1", email = "a@b.com", displayName = "Test"),
    )

    private fun tokenPairJson(access: String, refresh: String) = """
        {
          "access_token": "$access",
          "refresh_token": "$refresh",
          "user": { "id": "u1", "email": "a@b.com", "display_name": "Test" }
        }
    """.trimIndent()
}
