package app.protestmode.setting

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class BluetoothSettingTest {
    private val adapter: BluetoothAdapter get() = context.getSystemService(BluetoothManager::class.java).adapter

    @Test
    fun readsAdapterState() {
        shadowOf(adapter).setState(BluetoothAdapter.STATE_ON)
        assertEquals(SettingState.ON, BluetoothSetting.state(context))

        shadowOf(adapter).setState(BluetoothAdapter.STATE_TURNING_OFF)
        assertEquals(SettingState.OFF, BluetoothSetting.state(context))

        shadowOf(adapter).setState(BluetoothAdapter.STATE_OFF)
        assertEquals(SettingState.OFF, BluetoothSetting.state(context))
    }

    @Test
    fun availabilityFollowsHardware() {
        setFeature(PackageManager.FEATURE_BLUETOOTH, true)
        assertTrue(BluetoothSetting.isAvailable(context))

        setFeature(PackageManager.FEATURE_BLUETOOTH, false)
        assertFalse(BluetoothSetting.isAvailable(context))
    }

    @Test
    @Config(sdk = [30])
    fun disablesAutomaticallyUpToApi30() {
        shadowOf(adapter).setState(BluetoothAdapter.STATE_ON)
        shadowOf(adapter).setEnabled(true)

        assertTrue(BluetoothSetting.tryProtect(context))
        assertEquals(SettingState.OFF, BluetoothSetting.state(context))
    }

    @Test
    @Config(sdk = [31, 33])
    fun neverDisablesAutomaticallyFromApi31() {
        shadowOf(adapter).setState(BluetoothAdapter.STATE_ON)

        assertFalse(BluetoothSetting.tryProtect(context))
        assertEquals(SettingState.ON, BluetoothSetting.state(context))
    }

    @Test
    fun guidesToBluetoothScreen() {
        assertEquals(
            listOf(Settings.ACTION_BLUETOOTH_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS),
            BluetoothSetting.guideActions()
        )
    }
}
