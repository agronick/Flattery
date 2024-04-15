//Settings Application Kotlin

@file:Suppress("DEPRECATION")

package com.agronick.launcher.presentation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import com.agronick.launcher.R
import com.google.android.wearable.intent.RemoteIntent

class LauncherSettings : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher_settings)

        if (savedInstanceState == null) {
            fragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, LauncherSettingsFragment())
                .commit()
        }
    }
}

class LauncherSettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferencelayout)

        /* Find the feedback preference */
        val feedbackPreference = findPreference("feedback")

        /* Set an OnPreferenceClickListener to the feedback preference */
        feedbackPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val url = "https://github.com/agronick/Flattery/issues" // The URL you want to open
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)

            /* Use RemoteIntent to start the activity on the phone */
            RemoteIntent.startRemoteActivity(context, intent, null)

            true // Indicate that the click was handled
        }
    }
}