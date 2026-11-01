package com.hirephaze.steno.data

import android.content.Context

/** Small on-device preferences store. No cloud, no accounts. */
object Settings {
    private const val PREFS = "steno_prefs"
    private const val KEY_SMART_CASE = "smart_case"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Reproduce the trigger's capitalization on the expansion. Default on. */
    fun smartCase(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SMART_CASE, true)

    fun setSmartCase(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_SMART_CASE, value).apply()
}
