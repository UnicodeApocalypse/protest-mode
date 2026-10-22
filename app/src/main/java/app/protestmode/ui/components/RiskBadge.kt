package app.protestmode.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.protestmode.setting.RiskLevel
import app.protestmode.ui.a11yRes
import app.protestmode.ui.colors
import app.protestmode.ui.labelRes

@Composable
fun RiskBadge(risk: RiskLevel, active: Boolean, modifier: Modifier = Modifier) {
    val (container, content) = risk.colors()
    val description = stringResource(risk.a11yRes())
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) container else Color.Transparent,
        contentColor = if (active) content else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.semantics { contentDescription = description }
    ) {
        Text(
            text = stringResource(risk.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
