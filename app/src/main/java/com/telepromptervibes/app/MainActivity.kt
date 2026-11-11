package com.telepromptervibes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.telepromptervibes.app.ui.theme.TeleprompterTheme
import androidx.compose.material3.Surface
import com.telepromptervibes.app.ui.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeleprompterTheme {
                Surface {
                    AppNavHost()
                }
            }
        }
    }
}
