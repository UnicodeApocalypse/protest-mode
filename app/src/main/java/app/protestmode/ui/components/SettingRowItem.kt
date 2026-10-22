package app.protestmode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.setting.SettingCatalog
import app.protestmode.setting.SettingState
import app.protestmode.ui.SettingRow
import app.protestmode.ui.a11yRes
import app.protestmode.ui.stateRes
import app.protestmode.ui.theme.LocalStatusColors
import app.protestmode.ui.verdictRes

@Composable
fun SettingRowItem(row: SettingRow, onClick: () -> Unit, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val setting = SettingCatalog[row.id]
    val title = stringResource(setting.titleRes)
    val state = stringResource(row.stateRes())
    val verdict = stringResource(row.verdictRes())
    val risk = stringResource(row.id.risk.a11yRes())
    val instruction = setting.instructionRes?.takeUnless { row.isProtected }?.let { stringResource(it) }
    val description =
        listOfNotNull(stringResource(R.string.setting_row_a11y, title, state, verdict), risk, row.detail, instruction)
            .joinToString(", ")
    val needsConfirmation = row.state == SettingState.UNKNOWN && !row.confirmed
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(painterResource(setting.iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(row)
                    Text("$state · $verdict", style = MaterialTheme.typography.bodyMedium)
                }
                row.detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                instruction?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            RiskBadge(row.id.risk, active = !row.isProtected)
        }
        if (needsConfirmation) {
            OutlinedButton(
                onClick = onConfirm,
                modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 8.dp).heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        }
    }
}

@Composable
private fun StatusPill(row: SettingRow) {
    val colors = LocalStatusColors.current
    val (glyph, container, content) =
        when {
            row.isProtected -> Triple(R.drawable.ic_check, colors.discreet, colors.onDiscreet)
            row.state == SettingState.UNKNOWN -> Triple(
                R.drawable.ic_question,
                colors.unknown,
                MaterialTheme.colorScheme.surface
            )
            else -> Triple(R.drawable.ic_close, colors.exposed, colors.onExposed)
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier
            .size(18.dp)
            .background(container, CircleShape)
    ) {
        Icon(painterResource(glyph), contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
    }
}
