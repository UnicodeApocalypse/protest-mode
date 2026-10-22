package app.protestmode.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.exposure.ExposureStatus
import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.SettingId
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class MainViewModelTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val resolver get() = app.contentResolver

    @Suppress("DEPRECATION")
    @Before
    fun setUp() {
        val pm = shadowOf(app.packageManager)
        listOf(
            PackageManager.FEATURE_BLUETOOTH,
            PackageManager.FEATURE_BLUETOOTH_LE,
            PackageManager.FEATURE_NFC,
            PackageManager.FEATURE_TELEPHONY,
            "android.hardware.uwb"
        ).forEach { pm.setSystemFeature(it, false) }
        pm.setSystemFeature(PackageManager.FEATURE_WIFI, true)
        app.getSystemService(WifiManager::class.java).setWifiEnabled(true)
        shadowOf(app.getSystemService(LocationManager::class.java)).setLocationEnabled(false)
        Settings.Global.putInt(resolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        Settings.Global.putInt(resolver, "wifi_scan_always_enabled", 0)
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        MainViewModel(app, handle, Dispatchers.Main.immediate)

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

    private fun MainViewModel.nextGuide() = runBlocking { withTimeout(1_000) { guideRequests.first() } }

    @Test
    fun listsEveryAvailableSettingInTheFixedOrder() {
        val ids = viewModel().state.value.rows.map { it.id }

        assertEquals(SettingId.entries.filter { it in ids }, ids)
        assertFalse(SettingId.NFC in ids)
        assertTrue(SettingId.WIFI in ids)
    }

    @Test
    fun noScreenLockMeansExposed() {
        // The Robolectric device has no secure screen lock, a critical setting.
        assertEquals(ExposureStatus.EXPOSED, viewModel().state.value.exposure)
        assertFalse(ExposureLabel.DEVICE_ACCESS in viewModel().state.value.securedLabels)
    }

    @Test
    fun selectingALabelFiltersTheList() {
        val vm = viewModel()

        vm.toggleLabel(ExposureLabel.LOCATION_TRACKING)
        val rows = vm.state.value.rows
        assertEquals(ExposureLabel.LOCATION_TRACKING, vm.state.value.selectedLabel)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { ExposureLabel.LOCATION_TRACKING in it.id.labels })
        assertFalse(rows.any { it.id == SettingId.SCREEN_LOCK })

        vm.toggleLabel(ExposureLabel.LOCATION_TRACKING)
        assertNull(vm.state.value.selectedLabel)
        assertTrue(vm.state.value.rows.any { it.id == SettingId.SCREEN_LOCK })
    }

    @Test
    fun selectedLabelIsKeptInSavedState() {
        val handle = SavedStateHandle()
        viewModel(handle).toggleLabel(ExposureLabel.ACTIVITY_HISTORY)

        assertEquals(ExposureLabel.ACTIVITY_HISTORY, viewModel(handle).state.value.selectedLabel)
    }

    @Test
    fun resumeRefreshesStates() {
        val vm = viewModel()
        fun protectedIds() = vm.state.value.rows.filter { it.isProtected }.map { it.id }
        assertFalse(SettingId.AIRPLANE in protectedIds())
        assertFalse(SettingId.WIFI in protectedIds())

        Settings.Global.putInt(resolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        @Suppress("DEPRECATION")
        app.getSystemService(WifiManager::class.java).setWifiEnabled(false)
        vm.onResume()
        idle()

        assertTrue(SettingId.AIRPLANE in protectedIds())
        assertTrue(SettingId.WIFI in protectedIds())
    }

    @Test
    fun confirmingUnreadableSettingMarksItProtected() {
        shadowOf(app.packageManager).setSystemFeature("android.hardware.uwb", true)
        val vm = viewModel()

        vm.confirmRow(SettingId.UWB)
        vm.confirmRow(SettingId.WIFI)

        val rows = vm.state.value.rows.associateBy { it.id }
        assertTrue(rows.getValue(SettingId.UWB).confirmed)
        assertTrue(rows.getValue(SettingId.UWB).isProtected)
        assertFalse(rows.getValue(SettingId.WIFI).confirmed)
    }

    @Test
    fun renamingTheDeviceDropsItsConfirmation() {
        Settings.Global.putString(resolver, Settings.Global.DEVICE_NAME, "Galaxy S21")
        val vm = viewModel()
        fun deviceName() = vm.state.value.rows.first { it.id == SettingId.DEVICE_NAME }

        vm.confirmRow(SettingId.DEVICE_NAME)
        assertTrue(deviceName().confirmed)
        assertTrue(deviceName().isProtected)

        vm.onResume()
        idle()
        assertTrue("same name keeps the confirmation", deviceName().confirmed)

        Settings.Global.putString(resolver, Settings.Global.DEVICE_NAME, "Galaxy Alex")
        vm.onResume()
        idle()
        assertFalse(deviceName().confirmed)
        assertFalse(deviceName().isProtected)

        Settings.Global.putString(resolver, Settings.Global.DEVICE_NAME, "Galaxy S21")
        vm.onResume()
        idle()
        assertFalse("an old confirmation does not come back", deviceName().confirmed)
    }

    @Test
    fun confirmedDeviceNameIsNeverStoredInClear() {
        Settings.Global.putString(resolver, Settings.Global.DEVICE_NAME, "Pixel Alex")

        viewModel().confirmRow(SettingId.DEVICE_NAME)

        val stored = app.getSharedPreferences("confirmations", Context.MODE_PRIVATE).all
        assertEquals(setOf("DEVICE_NAME"), stored.keys)
        assertFalse(stored.toString(), "Pixel Alex" in stored.getValue("DEVICE_NAME").toString())
    }

    @Test
    fun deviceNameConfirmationSurvivesARestart() {
        Settings.Global.putString(resolver, Settings.Global.DEVICE_NAME, "Galaxy S21")
        viewModel().confirmRow(SettingId.DEVICE_NAME)

        val restarted = viewModel(SavedStateHandle())

        assertTrue(restarted.state.value.rows.first { it.id == SettingId.DEVICE_NAME }.confirmed)
    }

    @Test
    fun confirmationWithoutComparableValueOnlyLastsForTheSession() {
        shadowOf(app.packageManager).setSystemFeature("android.hardware.uwb", true)
        val handle = SavedStateHandle()
        viewModel(handle).confirmRow(SettingId.UWB)

        assertTrue(viewModel(handle).state.value.rows.first { it.id == SettingId.UWB }.confirmed)
        assertFalse(viewModel(SavedStateHandle()).state.value.rows.first { it.id == SettingId.UWB }.confirmed)
    }

    @Test
    fun rowTapRequestsGuidance() {
        val vm = viewModel()

        vm.onRowTapped(vm.state.value.rows.first { it.id == SettingId.WIFI })

        assertTrue(vm.nextGuide().action != null)
    }
}
