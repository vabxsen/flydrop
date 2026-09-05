package com.flydrop.app

import android.Manifest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-end smoke coverage for the primary guest navigation and actions. */
@RunWith(AndroidJUnit4::class)
class FlyDropNavigationTest {

    private val contactsPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(contactsPermissionRule)
        .around(composeRule)

    @Test
    fun guestFlowExercisesAllScreensAndActions() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Continue as guest")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Continue with Google")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeRule.onNodeWithText("Continue as guest").performClick()

        composeRule.onNodeWithText("Favourite Friends").assertIsDisplayed()
        composeRule.onNodeWithText("No favourite friends yet").assertIsDisplayed()
        composeRule.onNodeWithText("Contacts").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Unread notifications", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Notifications").performClick()
        composeRule.onNodeWithContentDescription("Unread notifications", useUnmergedTree = true)
            .assertDoesNotExist()

        composeRule.onNodeWithText("Send File").performClick()
        assertNearbyAndToggleDiscovery()
        addGofarAsFriend()
        navigateTo("Home")
        composeRule.onNodeWithText("Favourite Friends").assertIsDisplayed()

        composeRule.onNodeWithText("Receive File").performClick()
        composeRule.onNodeWithText("Nearby Friends").assertIsDisplayed()
        navigateTo("Home")

        composeRule.onNodeWithContentDescription("Scan a FlyDrop code").performClick()
        composeRule.onNodeWithText("Nearby Friends").assertIsDisplayed()
        navigateTo("Home")

        navigateTo("Profile")
        composeRule.onNodeWithText("Not signed in").assertIsDisplayed()
        openAboutAndSwitchTabs()
        composeRule.onNodeWithText("Sign in").performClick()
        composeRule.onNodeWithText("Sign in to continue").assertIsDisplayed()
    }

    /** About opens from Profile, both tabs render, and Back returns to Profile. */
    private fun openAboutAndSwitchTabs() {
        composeRule.onNodeWithText("About").performClick()

        // Version is the tab About opens on. "Build type" and "Package" appear
        // only in that pane, so they stand in for it.
        composeRule.onNodeWithText("Build type").assertIsDisplayed()
        composeRule.onNodeWithText("Package").assertIsDisplayed()

        aboutTab("Credits").assertIsNotSelected().performClick()
        aboutTab("Credits").assertIsSelected()
        composeRule.onNodeWithText("Poppins").assertIsDisplayed()
        composeRule.onNodeWithText("Build type").assertDoesNotExist()

        aboutTab("Version").performClick()
        composeRule.onNodeWithText("Build type").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Not signed in").assertIsDisplayed()
    }

    /**
     * The tab, not the "Version" row label that shares its text: only the tab
     * carries the Tab role.
     */
    private fun aboutTab(label: String) = composeRule.onNode(
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
    )

    private fun assertNearbyAndToggleDiscovery() {
        composeRule.onNodeWithText("Nearby Friends").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Device discovery")
            .assertIsOn()
            .performClick()
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    private fun addGofarAsFriend() {
        composeRule.onNodeWithContentDescription("Add Gofar Badman as friend")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription("Add Gofar Badman as friend")
            .assertDoesNotExist()
    }

    private fun navigateTo(destination: String) {
        composeRule.onNode(
            hasContentDescription(destination) and hasClickAction(),
        ).performClick()
    }
}
