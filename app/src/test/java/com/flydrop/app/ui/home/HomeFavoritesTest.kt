package com.flydrop.app.ui.home

import com.flydrop.app.data.model.FlyUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFavoritesTest {

    private val ada = FlyUser("contact:1", "Ada", "Phone contact", avatarSeed = 1)
    private val grace = FlyUser("contact:2", "Grace", "Phone contact", avatarSeed = 2)

    @Test
    fun favouritesAreEmptyUntilAnIdIsSelected() {
        assertTrue(favouriteContacts(listOf(ada, grace), emptySet()).isEmpty())
    }

    @Test
    fun selectingAndRemovingAContactUpdatesFavourites() {
        val selectedIds = toggleFavouriteId(emptySet(), grace.id)
        assertEquals(listOf(grace), favouriteContacts(listOf(ada, grace), selectedIds))

        val removedIds = toggleFavouriteId(selectedIds, grace.id)
        assertTrue(favouriteContacts(listOf(ada, grace), removedIds).isEmpty())
    }
}
