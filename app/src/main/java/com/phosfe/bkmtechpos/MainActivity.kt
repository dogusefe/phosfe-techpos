package com.phosfe.bkmtechpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.phosfe.bkmtechpos.ui.PhosfeTerminalApp
import com.phosfe.bkmtechpos.ui.theme.PhosfeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhosfeTheme {
                PhosfeTerminalApp()
            }
        }
    }
}

