package app.protestmode.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.protestmode.R
import app.protestmode.exposure.ExposureStatus
import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.RiskLevel
import app.protestmode.setting.SettingCatalog
import app.protestmode.setting.SettingCategory
import app.protestmode.setting.SettingState
import app.protestmode.setting.labelRes
import app.protestmode.ui.theme.LocalStatusColors

@StringRes
fun ExposureStatus.titleRes() = when (this) {
    ExposureStatus.EXPOSED -> R.string.status_exposed
    ExposureStatus.PARTIAL -> R.string.status_partial
    ExposureStatus.DISCREET -> R.string.status_discreet
}

@StringRes
fun ExposureStatus.descriptionRes() = when (this) {
    ExposureStatus.EXPOSED -> R.string.status_exposed_desc
    ExposureStatus.PARTIAL -> R.string.status_partial_desc
    ExposureStatus.DISCREET -> R.string.status_discreet_desc
}

@Composable
fun ExposureStatus.color(): Color = when (this) {
    ExposureStatus.EXPOSED -> LocalStatusColors.current.exposed
    ExposureStatus.PARTIAL -> LocalStatusColors.current.partial
    ExposureStatus.DISCREET -> LocalStatusColors.current.discreet
}

@Composable
fun ExposureStatus.onColor(): Color = when (this) {
    ExposureStatus.EXPOSED -> LocalStatusColors.current.onExposed
    ExposureStatus.PARTIAL -> LocalStatusColors.current.onPartial
    ExposureStatus.DISCREET -> LocalStatusColors.current.onDiscreet
}

@StringRes
fun ExposureLabel.titleRes() = when (this) {
    ExposureLabel.DEVICE_ACCESS -> R.string.label_device_access
    ExposureLabel.LOCATION_TRACKING -> R.string.label_location_tracking
    ExposureLabel.FINGERPRINTING -> R.string.label_fingerprinting
    ExposureLabel.ACTIVITY_HISTORY -> R.string.label_activity_history
}

@StringRes
fun SettingRow.stateRes() = if (state == SettingState.UNKNOWN && confirmed) {
    R.string.setting_state_confirmed
} else {
    SettingCatalog[id].labelRes(state)
}

@StringRes
fun SettingRow.verdictRes() = when {
    isProtected -> R.string.target_ok
    state == SettingState.UNKNOWN -> R.string.target_unknown
    id.category != SettingCategory.TRACKING -> R.string.target_fix
    id.desiredEnabled -> R.string.target_not_ok_enable
    else -> R.string.target_not_ok
}

@StringRes
fun SettingCategory.titleRes() = when (this) {
    SettingCategory.TRACKING -> R.string.category_tracking
    SettingCategory.SECURITY -> R.string.category_security
    SettingCategory.ANONYMITY -> R.string.category_anonymity
}

@StringRes
fun SettingCategory.descriptionRes() = when (this) {
    SettingCategory.TRACKING -> R.string.category_tracking_desc
    SettingCategory.SECURITY -> R.string.category_security_desc
    SettingCategory.ANONYMITY -> R.string.category_anonymity_desc
}

@StringRes
fun RiskLevel.labelRes() = when (this) {
    RiskLevel.CRITICAL -> R.string.risk_critical
    RiskLevel.HIGH -> R.string.risk_high
    RiskLevel.MODERATE -> R.string.risk_moderate
}

@StringRes
fun RiskLevel.a11yRes() = when (this) {
    RiskLevel.CRITICAL -> R.string.risk_critical_a11y
    RiskLevel.HIGH -> R.string.risk_high_a11y
    RiskLevel.MODERATE -> R.string.risk_moderate_a11y
}

@Composable
fun RiskLevel.colors(): Pair<Color, Color> {
    val colors = LocalStatusColors.current
    return when (this) {
        RiskLevel.CRITICAL -> colors.exposed to colors.onExposed
        RiskLevel.HIGH -> colors.partial to colors.onPartial
        RiskLevel.MODERATE -> colors.caution to colors.onCaution
    }
}
