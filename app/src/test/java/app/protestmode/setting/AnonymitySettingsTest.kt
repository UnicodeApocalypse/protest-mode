package app.protestmode.setting

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AnonymitySettingsTest {
    private val googleApp = VoiceAssistantSetting.ASSISTANT_PACKAGES.first()

    @Test
    fun genericDeviceNameIsCompliant() {
        assertEquals(SettingState.OFF, DeviceNameSetting.stateFor("Pixel 3 XL", "Google", "Pixel 3 XL"))
        assertEquals(SettingState.OFF, DeviceNameSetting.stateFor("google pixel 3 xl", "Google", "Pixel 3 XL"))
    }

    @Test
    fun personalDeviceNameIsDetected() {
        listOf("Pixel de Sam", "Sam's Pixel", "Sam’s Galaxy", "Téléphone d'Alex", "Galaxy di Marco").forEach {
            assertEquals(it, SettingState.ON, DeviceNameSetting.stateFor(it, "Google", "Pixel 3 XL"))
        }
    }

    @Test
    fun otherDeviceNamesMustBeChecked() {
        assertEquals(SettingState.UNKNOWN, DeviceNameSetting.stateFor("Galaxy S21", "samsung", "SM-G991B"))
        assertEquals(SettingState.UNKNOWN, DeviceNameSetting.stateFor("  ", "Google", "Pixel"))
        assertEquals(SettingState.UNKNOWN, DeviceNameSetting.stateFor(null, "Google", "Pixel"))
    }

    @Test
    fun deviceNameIsReadFromGlobalSettings() {
        Settings.Global.putString(context.contentResolver, Settings.Global.DEVICE_NAME, "Pixel de Sam")

        assertEquals(SettingState.ON, DeviceNameSetting.state(context))
        assertTrue(DeviceNameSetting.detail(context)!!.contains("Pixel de Sam"))
    }

    private fun installAssistant(micGranted: Boolean, enabled: Boolean = true) {
        val info =
            PackageInfo().apply {
                packageName = googleApp
                applicationInfo =
                    ApplicationInfo().apply {
                        packageName = googleApp
                        this.enabled = enabled
                        nonLocalizedLabel = "Google"
                    }
                requestedPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
                requestedPermissionsFlags = intArrayOf(if (micGranted) PackageInfo.REQUESTED_PERMISSION_GRANTED else 0)
            }
        shadowOf(context.packageManager).installPackage(info)
    }

    @Test
    fun voiceAssistantIsHiddenWhenNoAssistantIsInstalled() {
        assertFalse(VoiceAssistantSetting.isAvailable(context))
    }

    @Test
    fun voiceAssistantWithMicrophoneIsOn() {
        installAssistant(micGranted = true)

        assertTrue(VoiceAssistantSetting.isAvailable(context))
        assertEquals(SettingState.ON, VoiceAssistantSetting.state(context))
        assertTrue(VoiceAssistantSetting.detail(context)!!.contains("Google"))
        val guide = VoiceAssistantSetting.guideIntents(context).first()
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, guide.action)
        assertEquals(googleApp, guide.data?.schemeSpecificPart)
    }

    @Test
    fun voiceAssistantWithoutMicrophoneIsOff() {
        installAssistant(micGranted = false)

        assertEquals(SettingState.OFF, VoiceAssistantSetting.state(context))
    }

    @Test
    fun disabledVoiceAssistantIsOff() {
        installAssistant(micGranted = true, enabled = false)

        assertEquals(SettingState.OFF, VoiceAssistantSetting.state(context))
    }
}
