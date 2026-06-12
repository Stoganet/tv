package com.stoganet.tv.ui.auth

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuickConnectScreenTest {

    @Test
    fun loadingState_showsProgressIndicator() = runComposeUiTest {
        setContent { QuickConnectScreen(state = QuickConnectUiState(), onIntent = {}) }

        onNodeWithText("Sign in with Quick Connect").assertDoesNotExist()
        onNodeWithContentDescription("Retry").assertDoesNotExist()
    }

    @Test
    fun waitingState_showsCode() = runComposeUiTest {
        setContent {
            QuickConnectScreen(
                state = QuickConnectUiState(code = "ABC123", status = QuickConnectUiState.Status.WaitingForApproval),
                onIntent = {},
            )
        }

        onNodeWithText("ABC123").assertIsDisplayed()
        onNodeWithText("Sign in with Quick Connect").assertIsDisplayed()
    }

    @Test
    fun expiredState_showsRetryButton() = runComposeUiTest {
        setContent {
            QuickConnectScreen(
                state = QuickConnectUiState(status = QuickConnectUiState.Status.Expired),
                onIntent = {},
            )
        }

        onNodeWithText("Code expired").assertIsDisplayed()
        onNodeWithContentDescription("Retry").assertIsDisplayed()
    }

    @Test
    fun errorState_showsRetryButton() = runComposeUiTest {
        setContent {
            QuickConnectScreen(
                state = QuickConnectUiState(status = QuickConnectUiState.Status.Error),
                onIntent = {},
            )
        }

        onNodeWithText("Can't reach Stoganet").assertIsDisplayed()
        onNodeWithContentDescription("Retry").assertIsDisplayed()
    }

    @Test
    fun retryButton_triggersRetryIntent() = runComposeUiTest {
        var intentFired = false
        setContent {
            QuickConnectScreen(
                state = QuickConnectUiState(status = QuickConnectUiState.Status.Expired),
                onIntent = { if (it == QuickConnectIntent.Retry) intentFired = true },
            )
        }

        onNodeWithText("Retry").requestFocus()
        onNodeWithText("Retry").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertTrue(intentFired)
    }
}
