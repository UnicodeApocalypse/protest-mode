package app.protestmode.setting

import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo

@RunWith(AndroidJUnit4::class)
class MobileDataSettingTest {
    private val telephony get() = context.getSystemService(TelephonyManager::class.java)

    @Test
    @Config(sdk = [26, 33])
    fun readsTelephonyManagerFromApi26() {
        shadowOf(telephony).setDataEnabled(true)
        assertEquals(SettingState.ON, MobileDataSetting.state(context))

        shadowOf(telephony).setDataEnabled(false)
        assertEquals(SettingState.OFF, MobileDataSetting.state(context))
    }

    @Test
    @Config(sdk = [23])
    fun readsGlobalSettingBeforeApi26() {
        Settings.Global.putInt(context.contentResolver, "mobile_data", 1)
        assertEquals(SettingState.ON, MobileDataSetting.state(context))

        Settings.Global.putInt(context.contentResolver, "mobile_data", 0)
        assertEquals(SettingState.OFF, MobileDataSetting.state(context))
    }

    @Test
    @Config(sdk = [23])
    fun missingGlobalSettingIsUnknown() {
        assertEquals(SettingState.UNKNOWN, MobileDataSetting.state(context))
    }

    @Test
    fun availabilityRequiresTelephony() {
        setFeature(PackageManager.FEATURE_TELEPHONY, true)
        shadowOf(telephony).setPhoneType(TelephonyManager.PHONE_TYPE_GSM)
        assertTrue(MobileDataSetting.isAvailable(context))

        shadowOf(telephony).setPhoneType(TelephonyManager.PHONE_TYPE_NONE)
        assertFalse(MobileDataSetting.isAvailable(context))

        setFeature(PackageManager.FEATURE_TELEPHONY, false)
        assertFalse(MobileDataSetting.isAvailable(context))
    }

    @Test
    @Config(sdk = [29])
    fun usesInternetPanelFromApi29() {
        assertEquals(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, MobileDataSetting.guideActions().first())
    }

    @Test
    @Config(sdk = [28])
    fun usesDataScreenBeforeApi29() {
        assertEquals(
            listOf(Settings.ACTION_DATA_ROAMING_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS),
            MobileDataSetting.guideActions()
        )
    }

    @Test
    fun activeCellularNetworkCountsAsEnabledWhateverTheSim() {
        assertEquals(SettingState.ON, MobileDataSetting.stateFor(SettingState.OFF, cellularDataActive = true))
        assertEquals(SettingState.ON, MobileDataSetting.stateFor(SettingState.UNKNOWN, cellularDataActive = true))
        assertEquals(SettingState.ON, MobileDataSetting.stateFor(SettingState.ON, cellularDataActive = false))
        assertEquals(SettingState.OFF, MobileDataSetting.stateFor(SettingState.OFF, cellularDataActive = false))
        assertEquals(SettingState.UNKNOWN, MobileDataSetting.stateFor(SettingState.UNKNOWN, cellularDataActive = false))
    }

    @Suppress("DEPRECATION")
    @Test
    fun detectsCellularInternetNetwork() {
        val connectivity = shadowOf(context.getSystemService(ConnectivityManager::class.java))
        connectivity.clearAllNetworks()
        assertFalse(MobileDataSetting.cellularDataActive(context))

        val network = ShadowNetwork.newInstance(42)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        connectivity.addNetwork(
            network,
            ShadowNetworkInfo.newInstance(null, ConnectivityManager.TYPE_MOBILE, 0, true, NetworkInfo.State.CONNECTED)
        )
        connectivity.setNetworkCapabilities(network, capabilities)

        assertTrue(MobileDataSetting.cellularDataActive(context))
        assertEquals(SettingState.ON, MobileDataSetting.state(context))
        assertEquals(context.getString(R.string.detail_cellular_data_active), MobileDataSetting.detail(context))
    }

    @Test
    fun airplaneModeCutsMobileDataWhateverTheToggle() {
        shadowOf(telephony).setDataEnabled(true)
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)

        assertEquals(SettingState.OFF, MobileDataSetting.state(context))
        assertEquals(context.getString(R.string.detail_mobile_data_airplane), MobileDataSetting.detail(context))
    }
}
