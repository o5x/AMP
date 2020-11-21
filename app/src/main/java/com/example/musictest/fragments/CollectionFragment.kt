package com.example.musictest.fragments

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.musictest.R
import com.example.musictest.activities.MainActivity
import com.example.musictest.activities.syncMusicController

class CollectionFragment : Fragment() {
    lateinit var imageButtonDownload : ImageButton


    private fun addListItem(
            fm: androidx.fragment.app.FragmentManager?,
            layout_id: Int,
    ) : ListerRecyclerFragment
    {
        val fragOne: Fragment = ListerRecyclerFragment()
        val tr = fm!!.beginTransaction()
        tr.add(layout_id, fragOne)
        tr.commitAllowingStateLoss()
        tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)

        return fragOne as ListerRecyclerFragment
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {

        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_collection, container, false)

        imageButtonDownload = v.findViewById(R.id.imageButtonDownload)
        imageButtonDownload.setOnClickListener {
            //download("https://cdn.arrol.fr/music/Legendary - Welshly Arms.mp3");
            download("https://cdn.arrol.fr/music/Kaleo%20-%20Way%20Down%20We%20Go.flac");
        }

        val fm = childFragmentManager

        // init with all ids
        addListItem(fm, R.id.collectionPlaylists).initPlaylistList(syncMusicController.getPlaylistsIds())

        return v;
    }

    private fun download(url: String) {

        if(URLUtil.isValidUrl(url))
        {
            Toast.makeText(context, "Downloading file", Toast.LENGTH_SHORT).show();
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
        }
        else
        {
            Toast.makeText(context, "CANNOT download file", Toast.LENGTH_SHORT).show();
        }
    }

    override fun onStart() {
        super.onStart()

        (activity as MainActivity).currentfragment = this

        (activity as MainActivity).button_back.visibility = View.INVISIBLE
        (activity as MainActivity).button_settings.visibility = View.VISIBLE
        (activity as MainActivity).title.text = "Collection"

        (activity as MainActivity).btn_home.colorFilter = null
        (activity as MainActivity).btn_search.colorFilter = null
        (activity as MainActivity).btn_collection.setColorFilter(R.color.th)
    }
}