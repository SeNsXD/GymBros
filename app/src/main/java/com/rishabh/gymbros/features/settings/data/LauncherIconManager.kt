package com.rishabh.gymbros.features.settings.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.rishabh.gymbros.features.settings.model.UiVersion

/**
 * Keeps the launcher identity aligned with the selected interface version.
 *
 * The aliases are declared in AndroidManifest.xml. We enable the desired alias
 * before disabling the other one so there is always a launcher entry available.
 */
internal object LauncherIconManager {
    private const val CLASSIC_ALIAS = "com.rishabh.gymbros.ClassicLauncher"
    private const val UI2_ALIAS = "com.rishabh.gymbros.Ui2Launcher"

    fun applyForUiVersion(context: Context, version: UiVersion) {
        val appContext = context.applicationContext
        val desired = UI2_ALIAS
        val other = CLASSIC_ALIAS

        setEnabled(appContext, desired, true)
        setEnabled(appContext, other, false)
    }

    private fun setEnabled(context: Context, componentClassName: String, enabled: Boolean) {
        val packageManager = context.packageManager
        val component = ComponentName(context.packageName, componentClassName)
        val targetState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        if (packageManager.getComponentEnabledSetting(component) != targetState) {
            packageManager.setComponentEnabledSetting(
                component,
                targetState,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
