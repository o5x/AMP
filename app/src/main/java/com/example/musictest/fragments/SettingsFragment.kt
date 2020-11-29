package com.example.musictest.fragments

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import com.example.musictest.R
import com.example.musictest.activities.MainActivity

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).apply {
            tvTitle.text = "Search"
            btnBack.visibility = View.VISIBLE
            btnSettings.visibility = View.INVISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        (activity as MainActivity).apply {
            btnSettings.visibility = View.VISIBLE
        }
    }
}

