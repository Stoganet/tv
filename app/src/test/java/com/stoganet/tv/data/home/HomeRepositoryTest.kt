package com.stoganet.tv.data.home

import com.stoganet.tv.api.model.HomeResponse
import com.stoganet.tv.data.net.StoganetApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryTest {

    private val api = mockk<StoganetApi>()
    private val repository = HomeRepository(api)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getHome returns success when api succeeds`() = runTest {
        coEvery { api.getHome() } returns HomeResponse(sections = emptyList())

        val result = repository.getHome()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `getHome returns failure when api throws`() = runTest {
        coEvery { api.getHome() } throws RuntimeException("network error")

        val result = repository.getHome()

        assertTrue(result.isFailure)
    }
}
