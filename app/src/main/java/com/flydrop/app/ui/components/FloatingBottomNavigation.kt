package com.flydrop.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/** A destination in the floating navigation bar. */
data class NavTab(
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector = icon,
)

/**
 * The floating pill navigation from the reference: a pale rounded container
 * inset from the screen edges, with the active destination expanding into a
 * violet pill that carries both icon and label while the others stay icon-only.
 *
 * A stock [androidx.compose.material3.NavigationBar] is full-bleed, opaque and
 * labels every item, so it cannot produce this.
 */
@Composable
fun FloatingBottomNavigation(
    tabs: List<NavTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = FlyDrop.dimens
    val shape = FlyDrop.shapes.navContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.navHeight)
            .shadow(10.dp, shape, clip = false, ambientColor = NavShadow, spotColor = NavShadow)
            .clip(shape)
            .background(FlyDrop.colors.navContainer, shape)
            .padding(dimens.navInnerPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            NavItem(
                tab = tab,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // The active pill takes noticeably more room than the icon-only tabs,
    // matching the proportions in the reference.
    val weight by animateFloatAsState(
        targetValue = if (selected) 1.9f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "navWeight",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) FlyDrop.colors.violet else Color.Transparent,
        animationSpec = tween(durationMillis = 280),
        label = "navContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else FlyDrop.colors.iconMuted,
        animationSpec = tween(durationMillis = 280),
        label = "navContentColor",
    )
    val haptics = LocalHapticFeedback.current
    val shape = FlyDrop.shapes.navPill
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .clip(shape)
            .background(containerColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = if (selected) Color.White else FlyDrop.colors.violet),
            ) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) tab.activeIcon else tab.icon,
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(FlyDrop.dimens.navIcon),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(200, delayMillis = 60)) +
                expandHorizontally(tween(280), expandFrom = Alignment.Start),
            exit = fadeOut(tween(120)) +
                shrinkHorizontally(tween(240), shrinkTowards = Alignment.Start),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tab.label,
                    style = FlyDrop.type.buttonLabel,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
    }
}

private val NavShadow = Color(0x225B6BA8)

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun FloatingBottomNavigationPreview() {
    FlyDropTheme {
        val tabs = listOf(
            NavTab("Home", FlyDropIcons.Home),
            NavTab("Nearby", FlyDropIcons.Globe, activeIcon = FlyDropIcons.Radar),
            NavTab("Profile", FlyDropIcons.Person),
        )
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FloatingBottomNavigation(tabs = tabs, selectedIndex = 0, onSelect = {})
            FloatingBottomNavigation(tabs = tabs, selectedIndex = 1, onSelect = {})
            FloatingBottomNavigation(tabs = tabs, selectedIndex = 2, onSelect = {})
        }
    }
}
