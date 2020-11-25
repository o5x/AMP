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
            tv_title.text = "Search"
            btn_back.visibility = View.VISIBLE
            btn_settings.visibility = View.INVISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        (activity as MainActivity).apply {
            btn_settings.visibility = View.VISIBLE
        }
    }
}

