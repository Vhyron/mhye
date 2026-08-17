package com.vhyron.mhye

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.vhyron.mhye.ui.subscriptions.SubscriptionListScreen
import com.vhyron.mhye.ui.theme.MhyeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MhyeTheme {
                SubscriptionListScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
