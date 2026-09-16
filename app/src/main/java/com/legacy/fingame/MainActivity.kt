package com.legacy.fingame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.legacy.fingame.ui.FinGameApp
import com.legacy.fingame.ui.theme.FinGameTheme

class MainActivity : ComponentActivity() {
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
