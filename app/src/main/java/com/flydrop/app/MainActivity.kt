package com.flydrop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.flydrop.app.ui.navigation.FlyDropApp
import com.flydrop.app.ui.theme.FlyDropTheme
import com.flydrop.app.ui.theme.NearbyBackground

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent bars with dark icons: the pale backgrounds run all the way
        // to the edges, as they do in the reference.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            FlyDropTheme {
                FlyDropApp(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NearbyBackground),
                )
            }
        }
    }

    private companion object {
        const val TRANSPARENT = android.graphics.Color.TRANSPARENT
    }
}
