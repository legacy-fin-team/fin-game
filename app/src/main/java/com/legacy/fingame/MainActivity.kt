package com.legacy.fingame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.legacy.fingame.ui.FinGameApp
import com.legacy.fingame.ui.theme.FinGameTheme

/** App entry point: sets up edge-to-edge display and hosts [FinGameApp] under [FinGameTheme]. */
class MainActivity : ComponentActivity() {
    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState previously saved state, or `null` on a fresh start.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinGameTheme {
                FinGameApp()
            }
        }
    }
}
