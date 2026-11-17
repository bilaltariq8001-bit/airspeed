package com.velocity.speedometer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.velocity.speedometer.domain.model.UserSettings
import com.velocity.speedometer.domain.repository.SettingsRepository
import com.velocity.speedometer.ui.navigation.VelocityNavGraph
import com.velocity.speedometer.ui.navigation.VelocityRoute
import com.velocity.speedometer.ui.screens.permission.checkCoarseLocation
import com.velocity.speedometer.ui.screens.permission.checkFineLocation
import com.velocity.speedometer.ui.theme.VelocityTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hasLocationPermission = checkFineLocation(this) || checkCoarseLocation(this)
        val startRoute = if (hasLocationPermission) {
            VelocityRoute.Speedometer.route
        } else {
            VelocityRoute.Permission.route
        }

        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = UserSettings()
            )
            VelocityTheme(themeMode = settings.themeMode) {
                val navController = rememberNavController()
                VelocityNavGraph(
                    navController = navController,
                    startDestination = startRoute
                )
            }
        }
    }
}
