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
import com.pepperonas.brutus.viewmodel.AlarmViewModel
import com.pepperonas.brutus.viewmodel.StopwatchViewModel
import com.pepperonas.brutus.viewmodel.TimerViewModel

private enum class HomeTab(
    val route: String,
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
) {
    ALARM("alarm", "Alarm", Icons.Filled.Alarm, Icons.Outlined.Alarm),
    WORLD("world", "Weltuhr", Icons.Filled.Language, Icons.Outlined.Language),
    STOPWATCH("stopwatch", "Stoppuhr", Icons.Filled.Timer, Icons.Outlined.Timer),
    TIMER("timer", "Timer", Icons.Filled.HourglassBottom, Icons.Outlined.HourglassBottom),
}

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
        fun tabIndex(entry: NavBackStackEntry): Int =
            HomeTab.entries.indexOfFirst { it.route == entry.destination.route }

        NavHost(
            navController = navController,
            startDestination = HomeTab.ALARM.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = {
                val dir = if (tabIndex(targetState) >= tabIndex(initialState)) 1 else -1
                slideInHorizontally(spatialSpec) { full -> dir * full / 8 } +
                    fadeIn(tween(220, delayMillis = 40))
            },
            exitTransition = {
                val dir = if (tabIndex(targetState) >= tabIndex(initialState)) 1 else -1
                slideOutHorizontally(spatialSpec) { full -> -dir * full / 8 } +
                    fadeOut(tween(110))
            },
            popEnterTransition = {
                val dir = if (tabIndex(targetState) >= tabIndex(initialState)) 1 else -1
                slideInHorizontally(spatialSpec) { full -> dir * full / 8 } +
                    fadeIn(tween(220, delayMillis = 40))
            },
            popExitTransition = {
                val dir = if (tabIndex(targetState) >= tabIndex(initialState)) 1 else -1
                slideOutHorizontally(spatialSpec) { full -> -dir * full / 8 } +
                    fadeOut(tween(110))
            },
        ) {
            composable(HomeTab.ALARM.route) { AlarmListScreen(viewModel = viewModel) }
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
                label = { Text(tab.label) },
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
