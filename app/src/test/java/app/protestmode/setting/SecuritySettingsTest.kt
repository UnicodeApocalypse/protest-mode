package app.protestmode.setting

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.hardware.biometrics.BiometricManager
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.R
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBiometricManager

@RunWith(AndroidJUnit4::class)
class SecuritySettingsTest {
    @Test
    fun screenLockFollowsKeyguard() {
        val keyguard = shadowOf(context.getSystemService(KeyguardManager::class.java))

        keyguard.setIsDeviceSecure(true)
        assertEquals(SettingState.ON, ScreenLockSetting.state(context))

        keyguard.setIsDeviceSecure(false)
        assertEquals(SettingState.OFF, ScreenLockSetting.state(context))
        assertEquals(R.string.state_not_secure, ScreenLockSetting.labelRes(SettingState.OFF))
    }

    @Test
    fun screenLockGuidesToPasswordSetup() {
        assertEquals(DevicePolicyManager.ACTION_SET_NEW_PASSWORD, ScreenLockSetting.guideActions().first())
    }

    @Test
    @Config(sdk = [30, 33])
    fun biometricsFollowBiometricManager() {
        val manager = Shadow.extract<ShadowBiometricManager>(context.getSystemService(BiometricManager::class.java))

        manager.setCanAuthenticate(true)
        assertEquals(SettingState.ON, BiometricsSetting.state(context))
        assertTrue(BiometricsSetting.isAvailable(context))

        manager.setCanAuthenticate(false)
        assertFalse(BiometricsSetting.isAvailable(context))
    }

    @Test
    fun lockscreenNotificationsCombineBothKeys() {
        val secure = SettingState.ON
        val open = SettingState.OFF
        val stateFor = LockscreenNotificationsSetting::stateFor
        assertEquals(SettingState.ON, stateFor(1, 1, secure))
        assertEquals(SettingState.OFF, stateFor(0, 1, secure))
        assertEquals(SettingState.OFF, stateFor(0, 1, open))
        assertEquals(SettingState.OFF, stateFor(1, 0, secure))
        assertEquals(SettingState.OFF, stateFor(null, 0, secure))
        assertEquals(SettingState.UNKNOWN, stateFor(null, null, secure))
        assertEquals(SettingState.UNKNOWN, stateFor(1, null, secure))
    }

    @Test
    fun hidingSensitiveContentProtectsNothingWithoutScreenLock() {
        assertEquals(SettingState.ON, LockscreenNotificationsSetting.stateFor(1, 0, SettingState.OFF))
        assertEquals(SettingState.UNKNOWN, LockscreenNotificationsSetting.stateFor(1, 0, SettingState.UNKNOWN))
    }

    @Test
    fun hidingSilentNotificationsOnlyIsNotEnough() {
        Settings.Secure.putInt(context.contentResolver, "lock_screen_show_notifications", 1)
        Settings.Secure.putInt(context.contentResolver, "lock_screen_allow_private_notifications", 1)
        Settings.Secure.putInt(context.contentResolver, "lock_screen_show_silent_notifications", 0)

        assertEquals(SettingState.ON, LockscreenNotificationsSetting.state(context))
        assertEquals(
            context.getString(R.string.detail_lockscreen_notifications),
            LockscreenNotificationsSetting.detail(context)
        )
    }

    @Test
    fun lockDelayAddsScreenTimeoutAndLockAfterDelay() {
        val secure = SettingState.ON
        assertEquals(SettingState.OFF, LockDelaySetting.stateFor(30_000, 0, secure))
        assertEquals(SettingState.OFF, LockDelaySetting.stateFor(55_000, 5_000, secure))
        assertEquals(SettingState.OFF, LockDelaySetting.stateFor(60_000, 0, secure))
        assertEquals(SettingState.ON, LockDelaySetting.stateFor(60_000, 5_000, secure))
        assertEquals(SettingState.ON, LockDelaySetting.stateFor(30_000, 600_000, secure))
        assertEquals(SettingState.ON, LockDelaySetting.stateFor(120_000, 0, secure))
    }

    @Test
    fun missingLockAfterDelayUsesAndroidDefault() {
        assertEquals(SettingState.OFF, LockDelaySetting.stateFor(55_000, null, SettingState.ON))
        assertEquals(SettingState.ON, LockDelaySetting.stateFor(60_000, null, SettingState.ON))
    }

    @Test
    fun phoneWithoutScreenLockNeverLocks() {
        assertEquals(SettingState.ON, LockDelaySetting.stateFor(15_000, 0, SettingState.OFF))
    }

    @Test
    fun lockDelayEdgeCases() {
        assertEquals(SettingState.ON, LockDelaySetting.stateFor(Int.MAX_VALUE, 0, SettingState.ON))
        assertEquals(SettingState.ON, LockDelaySetting.stateFor(-1, 0, SettingState.ON))
        assertEquals(SettingState.UNKNOWN, LockDelaySetting.stateFor(null, 0, SettingState.ON))
        assertEquals(SettingState.UNKNOWN, LockDelaySetting.stateFor(15_000, 0, SettingState.UNKNOWN))
        assertEquals(SettingState.ON, LockDelaySetting.stateFor(120_000, 0, SettingState.UNKNOWN))
    }

    @Test
    fun lockDelayDetailExplainsTheComputation() {
        val keyguard = shadowOf(context.getSystemService(KeyguardManager::class.java))
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30_000)

        keyguard.setIsDeviceSecure(false)
        assertEquals(SettingState.ON, LockDelaySetting.state(context))
        assertEquals(context.getString(R.string.detail_lock_never), LockDelaySetting.detail(context))

        keyguard.setIsDeviceSecure(true)
        Settings.Secure.putLong(context.contentResolver, "lock_screen_lock_after_timeout", 0)
        assertEquals(SettingState.OFF, LockDelaySetting.state(context))
        assertEquals(
            context.getString(R.string.detail_lock_delay_immediate, context.getString(R.string.duration_seconds, 30)),
            LockDelaySetting.detail(context)
        )
        assertEquals(Settings.ACTION_SECURITY_SETTINGS, LockDelaySetting.guideActions().first())

        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 1_800_000)
        assertEquals(SettingState.ON, LockDelaySetting.state(context))
        assertEquals(Settings.ACTION_DISPLAY_SETTINGS, LockDelaySetting.guideActions().first())
    }

    @Suppress("DEPRECATION")
    @Test
    fun encryptionStatuses() {
        val secure = SettingState.ON
        assertEquals(
            SettingState.ON,
            EncryptionSetting.stateFor(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER, secure)
        )
        assertEquals(SettingState.ON, EncryptionSetting.stateFor(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE, secure))
        assertEquals(
            SettingState.OFF,
            EncryptionSetting.stateFor(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY, secure)
        )
        assertEquals(
            SettingState.OFF,
            EncryptionSetting.stateFor(DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE, secure)
        )
        assertEquals(
            SettingState.UNKNOWN,
            EncryptionSetting.stateFor(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING, secure)
        )
    }

    @Test
    fun encryptionWithoutScreenLockProtectsNothing() {
        val status = DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
        assertEquals(SettingState.OFF, EncryptionSetting.stateFor(status, SettingState.OFF))
        assertEquals(SettingState.UNKNOWN, EncryptionSetting.stateFor(status, SettingState.UNKNOWN))
    }

    @Test
    fun encryptionReadsDevicePolicyManager() {
        val dpm = shadowOf(context.getSystemService(DevicePolicyManager::class.java))
        val keyguard = shadowOf(context.getSystemService(KeyguardManager::class.java))
        keyguard.setIsDeviceSecure(true)

        dpm.setStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY)
        assertEquals(SettingState.OFF, EncryptionSetting.state(context))
        assertEquals(context.getString(R.string.detail_encryption_default_key), EncryptionSetting.detail(context))

        dpm.setStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER)
        assertEquals(SettingState.ON, EncryptionSetting.state(context))
        assertNull(EncryptionSetting.detail(context))

        keyguard.setIsDeviceSecure(false)
        assertEquals(SettingState.OFF, EncryptionSetting.state(context))
        assertEquals(context.getString(R.string.detail_encryption_default_key), EncryptionSetting.detail(context))
    }

    @Test
    fun developerOptionsCombineKeys() {
        assertEquals(SettingState.ON, DeveloperOptionsSetting.stateFor(developer = 1, adb = 0, adbWifi = null))
        assertEquals(SettingState.ON, DeveloperOptionsSetting.stateFor(developer = 0, adb = 1, adbWifi = null))
        assertEquals(SettingState.ON, DeveloperOptionsSetting.stateFor(developer = 0, adb = 0, adbWifi = 1))
        assertEquals(SettingState.OFF, DeveloperOptionsSetting.stateFor(developer = null, adb = 0, adbWifi = null))
        assertEquals(
            SettingState.UNKNOWN,
            DeveloperOptionsSetting.stateFor(developer = null, adb = null, adbWifi = null)
        )
    }

    @Test
    fun developerOptionsReportUsbDebugging() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)
        Settings.Global.putInt(context.contentResolver, Settings.Global.ADB_ENABLED, 1)

        assertEquals(SettingState.ON, DeveloperOptionsSetting.state(context))
        assertEquals(context.getString(R.string.detail_usb_debugging), DeveloperOptionsSetting.detail(context))
        assertEquals(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, DeveloperOptionsSetting.guideActions().first())
    }

    @Test
    fun securityPatchAge() {
        val patch = "2024-01-05"
        val patchMs = 1_704_412_800_000L

        assertEquals(SettingState.ON, SecurityPatchSetting.stateFor(patch, patchMs + TimeUnit.DAYS.toMillis(30)))
        assertEquals(SettingState.ON, SecurityPatchSetting.stateFor(patch, patchMs + SecurityPatchSetting.MAX_AGE_MS))
        assertEquals(SettingState.OFF, SecurityPatchSetting.stateFor(patch, patchMs + TimeUnit.DAYS.toMillis(400)))
        assertEquals(SettingState.UNKNOWN, SecurityPatchSetting.stateFor("", patchMs))
        assertEquals(SettingState.UNKNOWN, SecurityPatchSetting.stateFor(null, patchMs))
        assertEquals(SettingState.UNKNOWN, SecurityPatchSetting.stateFor("not a date", patchMs))
    }

    @Test
    @Config(sdk = [33])
    fun advancedProtectionIsHiddenBeforeAndroid16() {
        assertFalse(AdvancedProtectionSetting.isAvailable(context))
        assertEquals(SettingState.UNKNOWN, AdvancedProtectionSetting.state(context))
    }
}
