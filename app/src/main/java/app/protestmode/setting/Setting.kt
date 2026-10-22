package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.protestmode.R

enum class SettingState { ON, OFF, UNKNOWN }

enum class SettingCategory { TRACKING, SECURITY, ANONYMITY }

enum class RiskLevel { CRITICAL, HIGH, MODERATE }

enum class ExposureLabel { DEVICE_ACCESS, LOCATION_TRACKING, FINGERPRINTING, ACTIVITY_HISTORY }

private typealias L = ExposureLabel

/** Declaration order is the display order of the list. */
enum class SettingId(
    val category: SettingCategory,
    val risk: RiskLevel,
    val labels: Set<ExposureLabel>,
    val desiredEnabled: Boolean = false
) {
    AIRPLANE(
        SettingCategory.TRACKING,
        RiskLevel.CRITICAL,
        setOf(L.LOCATION_TRACKING, L.FINGERPRINTING),
        desiredEnabled = true
    ),
    LOCATION(SettingCategory.TRACKING, RiskLevel.CRITICAL, setOf(L.LOCATION_TRACKING, L.ACTIVITY_HISTORY)),
    WIFI(SettingCategory.TRACKING, RiskLevel.HIGH, setOf(L.LOCATION_TRACKING, L.FINGERPRINTING)),
    BLUETOOTH(SettingCategory.TRACKING, RiskLevel.HIGH, setOf(L.LOCATION_TRACKING, L.FINGERPRINTING)),
    WIFI_SCAN(SettingCategory.TRACKING, RiskLevel.HIGH, setOf(L.LOCATION_TRACKING, L.ACTIVITY_HISTORY)),
    HOTSPOT(SettingCategory.TRACKING, RiskLevel.HIGH, setOf(L.LOCATION_TRACKING, L.FINGERPRINTING)),

    MOBILE_DATA(SettingCategory.TRACKING, RiskLevel.MODERATE, setOf(L.ACTIVITY_HISTORY)),
    BLE_SCAN(SettingCategory.TRACKING, RiskLevel.MODERATE, setOf(L.LOCATION_TRACKING, L.ACTIVITY_HISTORY)),
    NFC(SettingCategory.TRACKING, RiskLevel.MODERATE, setOf(L.FINGERPRINTING)),
    UWB(SettingCategory.TRACKING, RiskLevel.MODERATE, setOf(L.LOCATION_TRACKING)),
    SCREEN_LOCK(SettingCategory.SECURITY, RiskLevel.CRITICAL, setOf(L.DEVICE_ACCESS), desiredEnabled = true),
    ENCRYPTION(SettingCategory.SECURITY, RiskLevel.CRITICAL, setOf(L.DEVICE_ACCESS), desiredEnabled = true),
    DEVELOPER_OPTIONS(SettingCategory.SECURITY, RiskLevel.CRITICAL, setOf(L.DEVICE_ACCESS)),
    LOCKSCREEN_NOTIFICATIONS(SettingCategory.SECURITY, RiskLevel.HIGH, setOf(L.DEVICE_ACCESS)),
    SECURITY_PATCH(SettingCategory.SECURITY, RiskLevel.HIGH, setOf(L.DEVICE_ACCESS), desiredEnabled = true),
    BIOMETRICS(SettingCategory.SECURITY, RiskLevel.MODERATE, setOf(L.DEVICE_ACCESS)),
    LOCK_DELAY(SettingCategory.SECURITY, RiskLevel.MODERATE, setOf(L.DEVICE_ACCESS)),
    ADVANCED_PROTECTION(
        SettingCategory.SECURITY,
        RiskLevel.MODERATE,
        setOf(L.DEVICE_ACCESS, L.FINGERPRINTING),
        desiredEnabled = true
    ),
    DEVICE_NAME(SettingCategory.ANONYMITY, RiskLevel.HIGH, setOf(L.FINGERPRINTING)),
    VOICE_ASSISTANT(SettingCategory.ANONYMITY, RiskLevel.MODERATE, setOf(L.ACTIVITY_HISTORY))
}

val SettingId.protectiveState: SettingState get() = if (desiredEnabled) SettingState.ON else SettingState.OFF

fun SettingId.isSatisfiedBy(state: SettingState) = state == protectiveState

data class Reading(
    val state: SettingState,
    val detail: String? = null,
    /** A manual confirmation is dropped as soon as this value changes. Null means that nothing can be compared. */
    val confirmationKey: String? = null
)

class Probe(val context: Context) {
    val screenLock: SettingState by lazy { ScreenLockSetting.readScreenLock(context) }
    val airplane: SettingState by lazy { AirplaneSetting.readAirplane(context) }
}

fun Boolean?.toSettingState(): SettingState = when (this) {
    true -> SettingState.ON
    false -> SettingState.OFF
    null -> SettingState.UNKNOWN
}

interface Setting {
    val id: SettingId

    @get:StringRes val titleRes: Int

    @get:DrawableRes val iconRes: Int

    @get:StringRes val instructionRes: Int? get() = null

    @get:StringRes val onLabelRes: Int get() = R.string.setting_state_on

    @get:StringRes val offLabelRes: Int get() = R.string.setting_state_off

    @get:StringRes val unknownLabelRes: Int get() = R.string.setting_state_unknown

    val observedUris: List<Uri> get() = emptyList()

    fun isAvailable(context: Context): Boolean

    fun read(probe: Probe): Reading

    fun tryProtect(context: Context): Boolean = false

    fun guideIntents(context: Context): List<Intent>
}

@StringRes
fun Setting.labelRes(state: SettingState) = when (state) {
    SettingState.ON -> onLabelRes
    SettingState.OFF -> offLabelRes
    SettingState.UNKNOWN -> unknownLabelRes
}
