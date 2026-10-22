package app.protestmode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.protestmode.ui.ProtestModeApp
import app.protestmode.ui.theme.ProtestModeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ProtestModeTheme {
                ProtestModeApp()
            }
        }
    }
}
