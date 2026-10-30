package com.hirephaze.steno.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.hirephaze.steno.engine.StenoAccessibilityService

/** True when Steno's accessibility service is currently enabled by the user. */
fun isServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, StenoAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabled)
    for (component in splitter) {
        if (component.equals(expected, ignoreCase = true)) return true
    }
    return false
}
