package app.protestmode

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun label() = context.getString(R.string.label_location_tracking)

    @Test
    @Config(qualifiers = "en")
    fun english() = assertEquals("Location tracking", label())

    @Test
    @Config(qualifiers = "fr")
    fun french() = assertEquals("Suivi de la position", label())

    @Test
    @Config(qualifiers = "es")
    fun spanish() = assertEquals("Seguimiento de la ubicación", label())

    @Test
    @Config(qualifiers = "it")
    fun italian() = assertEquals("Tracciamento della posizione", label())

    @Test
    @Config(qualifiers = "de")
    fun unsupportedLanguageFallsBackToEnglish() = assertEquals("Location tracking", label())

    private fun appName() = context.getString(R.string.app_name)

    @Test
    @Config(qualifiers = "en")
    fun englishAppName() = assertEquals("Protest Mode", appName())

    @Test
    @Config(qualifiers = "fr")
    fun frenchAppName() = assertEquals("Mode Manif", appName())

    @Test
    @Config(qualifiers = "es")
    fun spanishAppName() = assertEquals("Modo Mani", appName())

    @Test
    @Config(qualifiers = "it")
    fun italianAppName() = assertEquals("Modalità Manifestazione", appName())
}
