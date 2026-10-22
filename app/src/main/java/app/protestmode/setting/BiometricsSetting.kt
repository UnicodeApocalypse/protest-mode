package app.protestmode.setting

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.os.Build
import android.provider.Settings
import app.protestmode.R

/** Some vendor face unlocks of the "convenience" class are not declared to Android and stay invisible. */
object BiometricsSetting : Setting {
    override val id = SettingId.BIOMETRICS
    override val titleRes = R.string.setting_biometrics
    override val iconRes = R.drawable.ic_fingerprint
    override val instructionRes = R.string.instruction_biometrics
    override val onLabelRes = R.string.state_enrolled
    override val offLabelRes = R.string.state_none

    override fun isAvailable(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        biometricStatus(context).let { it != null && it != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE }
    } else {
        legacyFingerprint(context, "isHardwareDetected") == true
    }

    override fun read(probe: Probe): Reading {
        val context = probe.context
        val state =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                when (biometricStatus(context)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> SettingState.ON
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> SettingState.OFF
                    else -> SettingState.UNKNOWN
                }
            } else {
                legacyFingerprint(context, "hasEnrolledFingerprints").toSettingState()
            }
        return Reading(state)
    }

    override fun guideIntents(context: Context): List<Intent> =
        listOf(Intent(HiddenActions.FINGERPRINT), Intent(Settings.ACTION_SECURITY_SETTINGS))

    // BiometricManager.canAuthenticate(int) appears in API 30; API 29 only offers the strong-only variant.
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun biometricStatus(context: Context): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val manager = context.getSystemService(BiometricManager::class.java) ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            } else {
                manager.canAuthenticate()
            }
        } catch (_: SecurityException) {
            null
        }
    }

    // FingerprintManager is absent from the SDK 37 stubs but public on the API 23-28 devices that use
    // this branch, so its methods are called by reflection.
    private fun legacyFingerprint(context: Context, method: String): Boolean? {
        val manager = context.getSystemService("fingerprint") ?: return null
        return try {
            manager.javaClass.getMethod(method).invoke(manager) as? Boolean
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
}
