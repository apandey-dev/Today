package com.minimal.today.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import java.util.Calendar

object IconHelper {

    fun updateDynamicIcon(context: Context) {
        try {
            val calendar = Calendar.getInstance()
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH) // 1 to 31
            val packageName = context.packageName
            val pm = context.packageManager

            val targetAlias = "$packageName.Day$currentDay"

            for (day in 1..31) {
                val aliasName = "$packageName.Day$day"
                val componentName = ComponentName(packageName, aliasName)
                val currentState = pm.getComponentEnabledSetting(componentName)

                if (aliasName == targetAlias) {
                    if (currentState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                        pm.setComponentEnabledSetting(
                            componentName,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                } else {
                    if (currentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            componentName,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
