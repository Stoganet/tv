package com.stoganet.tv.ui.player

import android.content.Context
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.stoganet.tv.R
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayerScreenTest {

    private val mockPlayer = mockk<ExoPlayer>(relaxed = true)

    @Test
    fun loadingState_showsProgressIndicator() = runComposeUiTest {
        setContent {
            PlayerScreen(state = PlayerUiState.Loading, player = mockPlayer, onBack = {})
        }

        onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate),
        ).assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() = runComposeUiTest {
        setContent {
            PlayerScreen(state = PlayerUiState.Error, player = mockPlayer, onBack = {})
        }

        val ctx = ApplicationProvider.getApplicationContext<Context>()
        onNodeWithText(ctx.getString(R.string.player_error_message)).assertIsDisplayed()
    }

    @Test
    fun errorState_showsBackButton() = runComposeUiTest {
        setContent {
            PlayerScreen(state = PlayerUiState.Error, player = mockPlayer, onBack = {})
        }

        val ctx = ApplicationProvider.getApplicationContext<Context>()
        onNodeWithText(ctx.getString(R.string.player_back_content_description)).assertIsDisplayed()
    }
}
