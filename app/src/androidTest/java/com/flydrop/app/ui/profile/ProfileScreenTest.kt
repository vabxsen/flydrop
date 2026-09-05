package com.flydrop.app.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.theme.FlyDropTheme
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun connectionFailureKeepsTheIdAndOffersRetry() {
        val message =
            "Could not reach FlyDrop. Check your internet connection, then tap Save to retry."
        composeRule.setContent {
            FlyDropTheme {
                ProfileScreen(
                    user = FlyUser(
                        id = "test-user",
                        name = "Test User",
                        flyId = "fly#oldname",
                        avatarSeed = 1,
                    ),
                    signedIn = true,
                    flyIdState = ProfileUiState(
                        flyId = "fly#oldname",
                        canEdit = true,
                        editorOpen = true,
                        input = "newname",
                        submissionError = message,
                    ),
                )
            }
        }

        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Save").assertDoesNotExist()
        composeRule.onNodeWithText("newname").assertIsDisplayed()
    }
}
