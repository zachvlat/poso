package com.zachvlat.howmuchgr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zachvlat.howmuchgr.ui.screen.MainScreen
import com.zachvlat.howmuchgr.ui.theme.HowmuchgrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HowmuchgrTheme {
                MainScreen()
            }
        }
    }
}
