package app.protestmode.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProtestModeApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    // Closing the notification shade or a floating panel gives the focus back without ON_RESUME.
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused) {
        if (focused) viewModel.onResume()
    }

    LaunchedEffect(viewModel) {
        viewModel.guideRequests.collect { intent ->
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: SecurityException) {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    MainScreen(
        state = state,
        onRowClick = viewModel::onRowTapped,
        onConfirm = viewModel::confirmRow,
        onLabelClick = viewModel::toggleLabel
    )
}
