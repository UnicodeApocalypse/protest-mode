package app.protestmode.setting

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingCatalogTest {
    @Test
    fun everyIdHasExactlyOneSetting() {
        assertEquals(SettingId.entries.toSet(), SettingCatalog.all.map { it.id }.toSet())
        assertEquals(SettingId.entries.size, SettingCatalog.all.size)
        SettingId.entries.forEach { assertEquals(it, SettingCatalog[it].id) }
    }

    @Test
    fun everySettingCanBeReadWithoutThrowing() {
        SettingCatalog.all.forEach { setting ->
            setting.isAvailable(context)
            assertTrue(setting.id.name, setting.state(context) in SettingState.entries)
            assertTrue(setting.id.name, setting.guideIntents(context).isNotEmpty())
        }
    }

    @Test
    fun everySettingHasTextAndIcon() {
        SettingCatalog.all.forEach { setting ->
            assertTrue(context.getString(setting.titleRes).isNotBlank())
            setting.instructionRes?.let { assertTrue(context.getString(it).isNotBlank()) }
            assertTrue(context.resources.getResourceName(setting.iconRes).contains("drawable/ic_"))
        }
    }
}
