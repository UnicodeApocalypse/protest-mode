package app.protestmode.setting

import android.net.Uri
import android.provider.Settings

/**
 * Undocumented settings screens exposed by AOSP Settings. Vendor builds may lack them, hence the
 * fallbacks of each setting. Every action opened by a setting must be listed in the <queries> block
 * of the manifest, otherwise package visibility hides it on API 30+ (checked by a unit test).
 */
internal object HiddenActions {
    const val LOCATION_SCANNING = "android.settings.LOCATION_SCANNING_SETTINGS"
    const val WIFI_SCANNING = "android.settings.WIFI_SCANNING_SETTINGS"
    const val BLUETOOTH_SCANNING = "android.settings.BLUETOOTH_SCANNING_SETTINGS"
    const val TETHER = "android.settings.TETHER_SETTINGS"
    const val UWB = "android.settings.UWB_SETTINGS"
    const val ADVANCED_CONNECTED_DEVICE = "android.settings.ADVANCED_CONNECTED_DEVICE_SETTINGS"
    const val FINGERPRINT = "android.settings.FINGERPRINT_SETTINGS"
    const val LOCK_SCREEN = "android.settings.LOCK_SCREEN_SETTINGS"
    const val NOTIFICATION = "android.settings.NOTIFICATION_SETTINGS"
    const val DEVICE_NAME = "android.settings.DEVICE_NAME"

    // Public only since API 36.1, but resolved by the system update screen of older releases.
    const val SYSTEM_UPDATE = "android.settings.SYSTEM_UPDATE_SETTINGS"
}

/** Hidden settings keys from AOSP, readable by third-party apps; vendor builds or newer releases may hide them. */
internal object HiddenKeys {
    const val WIFI_ON = "wifi_on"
    const val MOBILE_DATA = "mobile_data"
    const val WIFI_SCAN_ALWAYS_ENABLED = "wifi_scan_always_enabled"
    const val BLE_SCAN_ALWAYS_ENABLED = "ble_scan_always_enabled"
    const val ADB_WIFI_ENABLED = "adb_wifi_enabled"
    const val LOCK_SCREEN_SHOW_NOTIFICATIONS = "lock_screen_show_notifications"
    const val LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS = "lock_screen_allow_private_notifications"

    // Only exists once a secure lock screen has been set.
    const val LOCK_SCREEN_LOCK_AFTER_TIMEOUT = "lock_screen_lock_after_timeout"
}

internal fun globalUri(key: String): Uri = Settings.Global.getUriFor(key)

internal fun secureUri(key: String): Uri = Settings.Secure.getUriFor(key)

internal fun systemUri(key: String): Uri = Settings.System.getUriFor(key)
