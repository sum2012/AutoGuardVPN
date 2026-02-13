package com.autoguard.vpn.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autoguard.vpn.ui.screens.HomeScreen
import com.autoguard.vpn.ui.screens.ServerListScreen
import com.autoguard.vpn.ui.screens.SettingsScreen
import com.autoguard.vpn.ui.theme.AutoGuardVPNTheme
import com.autoguard.vpn.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity
 * Application entry point, responsible for UI initialization and navigation
 * Inherits from AppCompatActivity to support AppCompatDelegate locale management
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // VPN permission request Launcher
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle VPN permission result
    }

    // Notification permission request Launcher (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle notification permission
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request necessary permissions
        checkAndRequestPermissions()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            AutoGuardVPNTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = hiltViewModel()

                    // Navigation Host
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        // Home Screen
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToServerList = {
                                    navController.navigate("server_list")
                                }
                            )
                        }

                        // Server List Screen
                        composable("server_list") {
                            ServerListScreen(
                                viewModel = viewModel,
                                onDismiss = {
                                    navController.popBackStack()
                                },
                                onServerSelected = { server ->
                                    viewModel.selectServer(server)
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Settings Screen
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Check and request necessary permissions
     */
    private fun checkAndRequestPermissions() {
        // Check VPN permission
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        }

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Request VPN permission
     */
    fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        }
    }
}
