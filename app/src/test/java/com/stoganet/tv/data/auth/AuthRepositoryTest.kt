package com.stoganet.tv.data.auth

import com.stoganet.tv.api.model.TokenPair
import com.stoganet.tv.api.model.User
import com.stoganet.tv.data.net.StoganetApi
import com.stoganet.tv.data.net.configurePlugins
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthRepositoryTest {

    private val fakeUser = User(id = "u1", email = "a@b.com", displayName = "Test")

    private fun tokenPairJson(access: String, refresh: String) =
        """{"access_token":"$access","refresh_token":"$refresh","user":{"id":"u1","email":"a@b.com","display_name":"Test"}}"""

    private fun buildTestApi(engine: MockEngine): StoganetApi {
        val tokenStore = TokenStore(FakeDataStore())
        val client = HttpClient(engine) { configurePlugins(tokenStore, "http://test.local/") }
        return StoganetApi(client, "http://test.local/")
    }

    @Test
    fun `startQuickConnect returns success with code and pollToken`() = runTest {
        val engine = MockEngine {
            respond(
                """{"code":"ABC123","poll_token":"tok-xyz"}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = AuthRepository(buildTestApi(engine), TokenStore(FakeDataStore()))

        val result = repository.startQuickConnect()

        assertTrue(result.isSuccess)
        assertEquals("ABC123", result.getOrThrow().code)
        assertEquals("tok-xyz", result.getOrThrow().pollToken)
    }

    @Test
    fun `startQuickConnect returns failure on 503`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.ServiceUnavailable) }
        val repository = AuthRepository(buildTestApi(engine), TokenStore(FakeDataStore()))

        val result = repository.startQuickConnect()

        assertTrue(result.isFailure)
    }

    @Test
    fun `pollQuickConnect returns Success on 200`() = runTest {
        val engine = MockEngine {
            respond(
                tokenPairJson("at", "rt"),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = AuthRepository(buildTestApi(engine), TokenStore(FakeDataStore()))

        val result = repository.pollQuickConnect("tok-xyz")

        assertTrue(result is QuickConnectPollResult.Success)
        assertEquals("at", (result as QuickConnectPollResult.Success).tokens.accessToken)
    }

    @Test
    fun `pollQuickConnect returns Pending on 202`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.Accepted) }
        val repository = AuthRepository(buildTestApi(engine), TokenStore(FakeDataStore()))

        val result = repository.pollQuickConnect("tok-xyz")

        assertEquals(QuickConnectPollResult.Pending, result)
    }

    @Test
    fun `pollQuickConnect returns Expired on 410`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode(410, "Gone")) }
        val repository = AuthRepository(buildTestApi(engine), TokenStore(FakeDataStore()))

        val result = repository.pollQuickConnect("tok-xyz")

        assertEquals(QuickConnectPollResult.Expired, result)
    }

    @Test
    fun `logout returns success and clears TokenStore on 204`() = runTest {
        val tokenStore = TokenStore(FakeDataStore())
        tokenStore.saveTokens(TokenPair(accessToken = "at", refreshToken = "rt", user = fakeUser))
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        val repository = AuthRepository(buildTestApi(engine), tokenStore)

        val result = repository.logout("rt")

        assertTrue(result.isSuccess)
        assertNull(tokenStore.accessToken())
        assertNull(tokenStore.refreshToken())
    }

    @Test
    fun `logout returns failure and does not clear TokenStore on error`() = runTest {
        val tokenStore = TokenStore(FakeDataStore())
        tokenStore.saveTokens(TokenPair(accessToken = "at", refreshToken = "rt", user = fakeUser))
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val repository = AuthRepository(buildTestApi(engine), tokenStore)

        val result = repository.logout("rt")

        assertTrue(result.isFailure)
        assertEquals("at", tokenStore.accessToken())
    }
}
