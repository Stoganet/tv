package com.stoganet.tv.data.auth

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.stoganet.tv.api.DefaultApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class AuthRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: AuthRepository

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = AuthRepository(retrofit.create(DefaultApi::class.java), retrofit)
    }

    @AfterEach
    fun tearDown() = server.close()

    @Test
    fun `startQuickConnect returns success with code and pollToken`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"code":"ABC123","poll_token":"tok-xyz"}""",
            ),
        )

        val result = repository.startQuickConnect()

        assertTrue(result.isSuccess)
        assertEquals("ABC123", result.getOrThrow().code)
        assertEquals("tok-xyz", result.getOrThrow().pollToken)
    }

    @Test
    fun `startQuickConnect returns failure on 503`() = runTest {
        server.enqueue(MockResponse(code = 503))

        val result = repository.startQuickConnect()

        assertTrue(result.isFailure)
    }

    @Test
    fun `pollQuickConnect returns Success on 200`() = runTest {
        val tokenBody = """{"access_token":"at","refresh_token":"rt",""" +
            """"user":{"id":"u1","email":"a@b.com","display_name":"Test"}}"""
        server.enqueue(MockResponse(code = 200, body = tokenBody))

        val result = repository.pollQuickConnect("tok-xyz")

        assertTrue(result is QuickConnectPollResult.Success)
        assertEquals("at", (result as QuickConnectPollResult.Success).tokens.accessToken)
    }

    @Test
    fun `pollQuickConnect returns Pending on 202`() = runTest {
        server.enqueue(MockResponse(code = 202))

        val result = repository.pollQuickConnect("tok-xyz")

        assertEquals(QuickConnectPollResult.Pending, result)
    }

    @Test
    fun `pollQuickConnect returns Expired on 410`() = runTest {
        server.enqueue(MockResponse(code = 410))

        val result = repository.pollQuickConnect("tok-xyz")

        assertEquals(QuickConnectPollResult.Expired, result)
    }

    @Test
    fun `logout returns success on 204`() = runTest {
        server.enqueue(MockResponse(code = 204))

        val result = repository.logout("refresh-token")

        assertTrue(result.isSuccess)
    }
}
