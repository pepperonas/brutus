@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.pepperonas.brutus.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.ui.settings.SettingsScreen
import com.pepperonas.brutus.viewmodel.AlarmViewModel
import com.pepperonas.brutus.viewmodel.StopwatchViewModel
import com.pepperonas.brutus.viewmodel.TimerViewModel
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.pepperonas.brutus.R

private enum class HomeTab(
    val route: String,
    @StringRes val labelRes: Int,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
) {
    ALARM("alarm", R.string.nav_alarm, Icons.Filled.Alarm, Icons.Outlined.Alarm),
    WORLD("world", R.string.nav_world_clock, Icons.Filled.Language, Icons.Outlined.Language),
    STOPWATCH("stopwatch", R.string.nav_stopwatch, Icons.Filled.Timer, Icons.Outlined.Timer),
    TIMER("timer", R.string.nav_timer, Icons.Filled.HourglassBottom, Icons.Outlined.HourglassBottom),
}

private const val SETTINGS_ROUTE = "settings"

@Composable
fun HomeScreen(viewModel: AlarmViewModel) {
    val navController = rememberNavController()
    // Activity-scoped (created here, outside the NavHost) so running timers /
    // stopwatch measurements survive tab switches.
    val timerViewModel: TimerViewModel = viewModel()
    val stopwatchViewModel: StopwatchViewModel = viewModel()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val selectedRoute = HomeTab.entries.firstOrNull { tab ->
        backStack?.destination?.hierarchy?.any { it.route == tab.route } == true ||
            currentRoute == tab.route
    }?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BrutusNavigationBar(
                selectedRoute = selectedRoute,
                onSelect = { tab ->
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(HomeTab.ALARM.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { padding ->
        // Direction-aware shared-axis X between tabs: the incoming screen
        // slides a short distance from the side its tab sits on, springing
        // via the expressive spatial spec, while fades do the hand-over.
        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        // Fades from the theme's effects specs instead of hand-picked tweens (220/110 ms).
        val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val fastEffectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        // "Remove animations": the tab change keeps its hand-over fade but no longer moves.
        val reducedMotion = com.pepperonas.brutus.ui.theme.rememberReducedMotion()
        // Settings sits "after" every tab, so it slides in from the end and back out again.
        fun tabIndex(entry: NavBackStackEntry): Int =
            if (entry.destination.route == SETTINGS_ROUTE) HomeTab.entries.size
            else HomeTab.entries.indexOfFirst { it.route == entry.destination.route }

        NavHost(
            navController = navController,
            startDestination = HomeTab.ALARM.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = {
                val dir = if (tabIndex(targetState) >= tabIndex(initialState)) 1 else -1
                if (reducedMotion) fadeIn(effectsSpec) else slideInHorizontally(spatialSpec) { full -> dir * full / 8 } + fadeIn(effectsSpec)
            },
            exitTransition = {
                val dir = if (tabIndex(targetState) >= tabIndex(initialState)) 1 else -1
                if (reducedMotion) fadeOut(fastEffectsSpec) else slideOutHorizontally(spatialSpec) { full -> -dir * full / 8 } + fadeOut(fastEffectsSpec)
            },
            popEnterTransition = {
                val dir = if (tabIndex(targetState) >= tabIndex(initialState)) 1 else -1
                if (reducedMotion) fadeIn(effectsSpec) else slideInHorizontally(spatialSpec) { full -> dir * full / 8 } + fadeIn(effectsSpec)
            },
            popExitTransition = {
                val dir = if (tabIndex(targetState) >= tabIndex(initialState)) 1 else -1
                if (reducedMotion) fadeOut(fastEffectsSpec) else slideOutHorizontally(spatialSpec) { full -> -dir * full / 8 } + fadeOut(fastEffectsSpec)
            },
        ) {
            composable(HomeTab.ALARM.route) {
                AlarmListScreen(viewModel = viewModel, onOpenSettings = { navController.navigate(SETTINGS_ROUTE) })
            }
            composable(SETTINGS_ROUTE) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(HomeTab.WORLD.route) { WorldClockScreen() }
            composable(HomeTab.STOPWATCH.route) { StopwatchScreen(viewModel = stopwatchViewModel) }
            composable(HomeTab.TIMER.route) { TimerScreen(viewModel = timerViewModel) }
        }
    }
}

/**
 * Expressive bottom navigation: tonal container, role-based selection colors
 * (pill indicator in secondaryContainer — no hardcoded brand red), and a
 * filled/outlined icon swap with a soft spatial spring on selection.
 *
 * ShortNavigationBar over classic NavigationBar: the compact M3E bar with the
 * animated indicator. A WideNavigationRail variant is deliberately skipped —
 * Brutus is a portrait phone app (alarm/lock-screen flows), landscape tablets
 * are not a target.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BrutusNavigationBar(
    selectedRoute: String?,
    onSelect: (HomeTab) -> Unit,
) {
    ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        HomeTab.entries.forEach { tab ->
            val selected = selectedRoute == tab.route
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.88f,
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                label = "navIconScale"
            )
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                        // Label below is always visible — a contentDescription
                        // here would make TalkBack announce the tab twice.
                        contentDescription = null,
                        modifier = Modifier.scale(iconScale)
                    )
                },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}

@Preview(name = "Nav dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun NavBarPreviewDark() {
    BrutusTheme(darkTheme = true) {
        BrutusNavigationBar(selectedRoute = "alarm", onSelect = {})
    }
}

@Preview(name = "Nav light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun NavBarPreviewLight() {
    BrutusTheme(darkTheme = false) {
        BrutusNavigationBar(selectedRoute = "stopwatch", onSelect = {})
    }
}

@Preview(
    name = "Nav dynamic",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE,
)
@Composable
private fun NavBarPreviewDynamic() {
    BrutusTheme(darkTheme = true) {
        BrutusNavigationBar(selectedRoute = "timer", onSelect = {})
    }
}
