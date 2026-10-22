package app.protestmode.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.setting.ExposureLabel
import app.protestmode.ui.theme.LocalStatusColors
import app.protestmode.ui.titleRes

@Composable
fun SecuredLabels(
    secured: Set<ExposureLabel>,
    selected: ExposureLabel?,
    onToggle: (ExposureLabel) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposureLabel.entries.forEach { label ->
            val isSecured = label in secured
            val title = stringResource(label.titleRes())
            val description =
                stringResource(
                    if (isSecured) R.string.label_secured_a11y else R.string.label_not_secured_a11y,
                    title
                ) + if (label == selected) ", " + stringResource(R.string.label_filter_active_a11y) else ""
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            FilterChip(
                selected = label == selected,
                onClick = { onToggle(label) },
                label = { Text(title) },
                leadingIcon = {
                    Icon(
                        painterResource(if (isSecured) R.drawable.ic_check else R.drawable.ic_remove),
                        contentDescription = null,
                        tint = if (isSecured) LocalStatusColors.current.discreet else muted,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                colors =
                FilterChipDefaults.filterChipColors(
                    labelColor = if (isSecured) MaterialTheme.colorScheme.onSurface else muted
                ),
                modifier = Modifier.semantics { contentDescription = description }
            )
        }
    }
}
