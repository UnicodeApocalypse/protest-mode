package app.protestmode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.SettingId
import app.protestmode.ui.components.CategoryHeader
import app.protestmode.ui.components.SecuredLabels
import app.protestmode.ui.components.SettingRowItem
import app.protestmode.ui.components.StatusBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onRowClick: (SettingRow) -> Unit,
    onConfirm: (SettingId) -> Unit,
    onLabelClick: (ExposureLabel) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "banner") {
                StatusBanner(state.exposure, state.remainingAdvice, Modifier.padding(horizontal = 16.dp))
            }
            item(key = "labels") {
                SecuredLabels(
                    secured = state.securedLabels,
                    selected = state.selectedLabel,
                    onToggle = onLabelClick,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            state.rows.groupBy { it.id.category }.forEach { (category, rows) ->
                item(key = "category_${category.name}") {
                    CategoryHeader(category)
                }
                items(rows, key = { it.id.name }) { row ->
                    SettingRowItem(row = row, onClick = { onRowClick(row) }, onConfirm = { onConfirm(row.id) })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
