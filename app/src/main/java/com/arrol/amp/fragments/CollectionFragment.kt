package com.arrol.amp.fragments

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Button
import android.widget.TabHost
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.arrol.amp.R
import com.arrol.amp.activities.MainActivity
import com.arrol.amp.musics.ListId.Companion.ID_MUSIC_USER_PLAYLISTS
import kotlinx.android.synthetic.main.fragment_collection.*


class CollectionFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabs = view.findViewById(R.id.tabHost) as TabHost
        tabs.setup()

        val playlistTab = tabs.newTabSpec("Playlists")
        playlistTab.setContent(R.id.tab1)
        playlistTab.setIndicator("Playlists")
        tabs.addTab(playlistTab)

        val homeTab = tabs.newTabSpec("Artists")
        homeTab.setIndicator("Artists")
        homeTab.setContent(R.id.tab2)
        tabs.addTab(homeTab)

        val faqTab = tabs.newTabSpec("Albums")
        faqTab.setIndicator("Albums")
        faqTab.setContent(R.id.tab3)
        tabs.addTab(faqTab)

        // Add FAB action
        imageButtonTitle.setOnClickListener { view ->
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
            val inflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.alert_add, null)
            dialogBuilder.setView(dialogView)

            val alertDialog: AlertDialog = dialogBuilder.create()
            alertDialog.show()

            val ib = dialogView.findViewById<Button>(R.id.imageButtonDownload)
            ib.setOnClickListener {
                //download("https://cdn.arrol.fr/music/Legendary - Welshly Arms.mp3");
                download("https://cdn.arrol.fr/music/Kaleo%20-%20Way%20Down%20We%20Go.flac")
                alertDialog.dismiss()
            }
        }

        /*fun  inFromRightAnimation() : Animation
        {
            val inFromRight = TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
            inFromRight.duration = 150;
            inFromRight.interpolator = AccelerateInterpolator();
            return inFromRight;
        }

        fun  outToRightAnimation() : Animation
        {
            val inFromRight = TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
            inFromRight.duration = 150;
            inFromRight.interpolator = AccelerateInterpolator();
            return inFromRight;
        }

        var currentTab = tabs.currentTab

        tabs.setOnTabChangedListener(OnTabChangeListener {
            val currentView: View = tabs.currentView
            if (tabs.currentTab > currentTab) {
                currentView.animation = inFromRightAnimation()
            } else {
                currentView.animation = outToRightAnimation()
            }
            currentTab = tabs.currentTab
        })*/

        val fm = childFragmentManager

        ListerRecyclerFragment().addItem(fm, R.id.tab1)
            .initSyncListById(ID_MUSIC_USER_PLAYLISTS, false)

        /*ListerRecyclerFragment().addItem(fm, R.id.tab2)
            .initSyncListById(ID_MUSIC_ARTISTS, false)

        ListerRecyclerFragment().addItem(fm, R.id.tab3)
            .initSyncListById(ID_MUSIC_ALBUMS, false)*/

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_collection, container, false)
    }

    private fun download(url: String) {

        if (URLUtil.isValidUrl(url)) {
            Toast.makeText(context, "Downloading file", Toast.LENGTH_SHORT).show()
            val request = DownloadManager.Request(Uri.parse(url))
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)

            val name = URLUtil.guessFileName(url, null, null)
            //name = "musicdl.mp3"
            request.setDescription("Downloading...")

            request.setTitle(name)
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)

            val manager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        } else {
            Toast.makeText(context, "CANNOT download file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).apply {
            btnHome.colorFilter = null
            btnSearch.colorFilter = null
            btnCollection.setColorFilter(R.color.th)
        }
    }

}