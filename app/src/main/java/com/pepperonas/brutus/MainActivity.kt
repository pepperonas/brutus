package com.pepperonas.brutus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.pepperonas.brutus.scheduler.Rescheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.pepperonas.brutus.ui.screens.HomeScreen
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.viewmodel.AlarmViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AlarmViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, we proceed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[AlarmViewModel::class.java]

        requestPermissions()

        setContent {
            BrutusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    /**
     * Force stop, a revoked-and-restored exact-alarm permission or a backup restore leave alarms
     * "on" in the list that AlarmManager no longer knows. Re-registering on every return to the
     * app is cheap and idempotent (fixed request codes, FLAG_UPDATE_CURRENT).
     */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) { Rescheduler.rescheduleAll(applicationContext) }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
