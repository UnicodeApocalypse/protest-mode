package app.protestmode

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManifestTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Suppress("DEPRECATION")
    private fun requested(): List<String> = context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.GET_PERMISSIONS
    ).requestedPermissions.orEmpty().toList()

    @Test
    fun noInternetPermission() {
        assertFalse(Manifest.permission.INTERNET in requested())
    }

    @Test
    fun noDangerousPermission() {
        requested().forEach { name ->
            val info = runCatching { context.packageManager.getPermissionInfo(name, 0) }.getOrNull() ?: return@forEach
            val protection = protectionOf(info)
            assertFalse("$name is dangerous", protection == PermissionInfo.PROTECTION_DANGEROUS)
        }
    }

    @Suppress("DEPRECATION")
    private fun protectionOf(info: PermissionInfo): Int = if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.P
    ) {
        info.protection
    } else {
        info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
    }

    @Test
    fun backupIsDisabled() {
        val flags = context.applicationInfo.flags
        assertEquals(0, flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
}
