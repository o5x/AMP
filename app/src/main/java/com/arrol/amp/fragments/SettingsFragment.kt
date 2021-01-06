package com.arrol.amp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.arrol.amp.R
import com.arrol.amp.activities.MainActivity
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainctx = (activity as MainActivity).baseContext

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(mainctx)

        switch_scanMusicStartup.isChecked  = sharedPref.getBoolean("scanOnStart", true)

        switch_scanMusicStartup.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("scanOnStart", isChecked).apply()
        }

        button_scanMusicNow.setOnClickListener {
            (activity as MainActivity).scanMusics()
        }
    }
}