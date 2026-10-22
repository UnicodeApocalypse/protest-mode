package app.protestmode

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.setting.SettingCatalog
import app.protestmode.setting.VoiceAssistantSetting
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.w3c.dom.Element

@RunWith(AndroidJUnit4::class)
class ManifestPolicyTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun declaredActions(): Set<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            File("src/main/AndroidManifest.xml")
        )
        val queries = document.getElementsByTagName("queries").item(0) as Element
        val actions = queries.getElementsByTagName("action")
        return (0 until actions.length).map { (actions.item(it) as Element).getAttribute("android:name") }.toSet()
    }

    private fun guidedActions(): Set<String> {
        // An installed assistant makes VoiceAssistantSetting guide to its app details screen.
        val pkg = VoiceAssistantSetting.ASSISTANT_PACKAGES.first()
        shadowOf(context.packageManager).installPackage(
            PackageInfo().apply {
                packageName = pkg
                applicationInfo = ApplicationInfo().apply { packageName = pkg }
            }
        )
        return SettingCatalog.all.flatMap { it.guideIntents(context) }.mapNotNull(Intent::getAction).toSet()
    }

    @Test
    @Config(sdk = [23, 29, 36])
    fun everyGuidedScreenIsDeclaredInQueries() {
        val missing = guidedActions() - declaredActions()
        assertTrue("Add to <queries>: $missing", missing.isEmpty())
    }

    @Test
    @Config(sdk = [36])
    fun everyDeclaredScreenIsUsed() {
        val unused = declaredActions() - guidedActions() - Settings.ACTION_SETTINGS
        assertTrue("Unused in <queries>: $unused", unused.isEmpty())
    }

    @Test
    fun requestedPermissionsAreOnTheClosedList() {
        val allowed =
            File("../config/allowed-permissions.txt")
                .readLines()
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toSet()

        @Suppress("DEPRECATION")
        val requested =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                .orEmpty()
                .toSet()

        assertTrue(Manifest.permission.INTERNET !in requested)
        assertEquals(emptySet<String>(), requested - allowed)
    }
}
