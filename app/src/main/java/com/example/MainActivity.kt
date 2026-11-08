package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.service.LocationTrackingService
import com.example.ui.navigation.TaxiMeterNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MeterViewModel
import com.example.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val meterViewModel: MeterViewModel by viewModels()

    fun applyKeepScreenAwakeFlag() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun clearKeepScreenAwakeFlag() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun isScreenAwakeFlagApplied(): Boolean {
        return (window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.AppLanguageManager.init(this)
        enableEdgeToEdge()
        // Keep Android display awake while GET TAXI is open and visible in the foreground
        applyKeepScreenAwakeFlag()
        if (!LocationTrackingService.isTripActivelyRunningForBubble()) {
            com.example.service.FloatingSpeedOverlayManager.forceHide(this)
        }
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TaxiMeterNavGraph(
                        meterViewModel = meterViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure display stays awake whenever GET TAXI returns to foreground
        applyKeepScreenAwakeFlag()
        if (!LocationTrackingService.isTripActivelyRunningForBubble()) {
            com.example.service.FloatingSpeedOverlayManager.forceHide(this)
            return
        }
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            LocationTrackingService.refreshOverlay(this)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Automatically show floating speedometer bubble when user presses Home or switches apps during an active trip
        LocationTrackingService.onAppMinimized(this)
    }

    override fun onStop() {
        super.onStop()
        // Release Activity window keep-screen-on flag when leaving foreground;
        // if an active trip's floating bubble is visible, its overlay window maintains FLAG_KEEP_SCREEN_ON.
        if (!isChangingConfigurations) {
            clearKeepScreenAwakeFlag()
            LocationTrackingService.onAppMinimized(this)
        }
    }

    override fun onDestroy() {
        clearKeepScreenAwakeFlag()
        super.onDestroy()
    }
}
