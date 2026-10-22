package app.protestmode.setting

import android.content.Context
import android.content.Intent

object SettingCatalog {
    val all: List<Setting> =
        listOf(
            AirplaneSetting,
            LocationSetting,
            WifiSetting,
            BluetoothSetting,
            WifiScanSetting,
            HotspotSetting,
            MobileDataSetting,
            BleScanSetting,
            NfcSetting,
            UwbSetting,
            ScreenLockSetting,
            EncryptionSetting,
            DeveloperOptionsSetting,
            LockscreenNotificationsSetting,
            SecurityPatchSetting,
            BiometricsSetting,
            LockDelaySetting,
            AdvancedProtectionSetting,
            DeviceNameSetting,
            VoiceAssistantSetting
        )

    private val byId = all.associateBy { it.id }

    operator fun get(id: SettingId): Setting = byId.getValue(id)

    fun guideIntent(setting: Setting, context: Context): Intent =
        IntentResolver.resolve(context.packageManager, setting.guideIntents(context))
}

fun interface SettingSource {
    fun readAll(context: Context): Map<SettingId, Reading>

    companion object {
        val Device =
            SettingSource { context ->
                val probe = Probe(context)
                SettingCatalog.all
                    .filter { it.isAvailable(context) }
                    .sortedBy { it.id.ordinal }
                    .associate { it.id to it.read(probe) }
            }
    }
}
