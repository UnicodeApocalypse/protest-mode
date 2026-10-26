package app.protestmode.setting

import android.os.Build

// Vendor skins (One UI, HyperOS, ColorOS, OxygenOS, realme UI, Funtouch OS, MagicOS, XOS...) move and
// rename AOSP settings screens. Their instructions name no menu path: their localized labels could not
// be verified.
internal object VendorSkin {
    private val names =
        listOf(
            "samsung", "xiaomi", "redmi", "poco", "oppo", "oneplus", "realme", "vivo", "iqoo", "honor",
            "huawei", "tecno", "infinix", "itel"
        )

    fun matches(manufacturer: String = Build.MANUFACTURER, brand: String = Build.BRAND): Boolean {
        val device = listOf(manufacturer.lowercase(), brand.lowercase())
        return names.any { name -> device.any { it.startsWith(name) } }
    }
}
