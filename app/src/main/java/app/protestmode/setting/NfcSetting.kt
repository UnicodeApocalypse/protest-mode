package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import app.protestmode.R

object NfcSetting : Setting {
    override val id = SettingId.NFC
    override val titleRes = R.string.setting_nfc
    override val iconRes = R.drawable.ic_nfc

    override fun isAvailable(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) && adapter(context) != null

    override fun read(probe: Probe): Reading {
        val enabled =
            try {
                adapter(probe.context)?.isEnabled
            } catch (_: RuntimeException) {
                null
            }
        return Reading(enabled.toSettingState())
    }

    override fun guideIntents(context: Context): List<Intent> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Intent(Settings.Panel.ACTION_NFC))
        add(Intent(Settings.ACTION_NFC_SETTINGS))
        add(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    private fun adapter(context: Context): NfcAdapter? = NfcAdapter.getDefaultAdapter(context.applicationContext)
}
