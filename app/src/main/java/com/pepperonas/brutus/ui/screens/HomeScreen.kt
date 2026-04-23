package com.pepperonas.brutus.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pepperonas.brutus.ui.theme.BrutusRed
import com.pepperonas.brutus.viewmodel.AlarmViewModel

private enum class HomeTab(val route: String, val label: String, val icon: ImageVector) {
    ALARM("alarm", "Alarm", Icons.Filled.Alarm),
    WORLD("world", "Weltuhr", Icons.Filled.Language),
    STOPWATCH("stopwatch", "Stoppuhr", Icons.Filled.Timer),
    TIMER("timer", "Timer", Icons.Filled.HourglassBottom),
}

@Composable
fun HomeScreen(viewModel: AlarmViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                HomeTab.entries.forEach { tab ->
                    val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true ||
                        currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(HomeTab.ALARM.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrutusRed,
                            selectedTextColor = BrutusRed,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeTab.ALARM.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(HomeTab.ALARM.route) { AlarmListScreen(viewModel = viewModel) }
            composable(HomeTab.WORLD.route) { WorldClockScreen() }
            composable(HomeTab.STOPWATCH.route) { StopwatchScreen() }
            composable(HomeTab.TIMER.route) { TimerScreen() }
        }
    }
}
