package app.protestmode.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.exposure.ExposureStatus
import app.protestmode.ui.color
import app.protestmode.ui.descriptionRes
import app.protestmode.ui.onColor
import app.protestmode.ui.titleRes

@Composable
fun StatusBanner(status: ExposureStatus, remainingAdvice: Int, modifier: Modifier = Modifier) {
    Surface(
        color = status.color(),
        contentColor = status.onColor(),
        shape = MaterialTheme.shapes.large,
        modifier =
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(status.titleRes()),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(status.descriptionRes()), style = MaterialTheme.typography.bodyLarge)
            if (remainingAdvice > 0) {
                Text(
                    text = pluralStringResource(R.plurals.status_remaining_advice, remainingAdvice, remainingAdvice),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
