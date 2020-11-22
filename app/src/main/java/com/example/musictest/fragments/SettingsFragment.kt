package com.example.musictest.fragments

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import com.example.musictest.activities.MainActivity
import com.example.musictest.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onStart() {
        super.onStart()


        //(activity as MainActivity).button_back.visibility = View.VISIBLE
        //(activity as MainActivity).button_settings.visibility = View.INVISIBLE
        (activity as MainActivity).title.text = "Settings"
    }
}