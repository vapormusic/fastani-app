package com.lagradost.fastani.ui.downloads

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.cast.framework.CastContext
import com.lagradost.fastani.*
import com.lagradost.fastani.MainActivity.Companion.getColorFromAttr
import com.lagradost.fastani.ui.PlayerData
import com.lagradost.fastani.ui.result.ResultFragment
import com.lagradost.fastani.ui.result.ResultFragment.Companion.fixEpTitle
import com.lagradost.fastani.ui.result.ResultFragment.Companion.isInResults
import kotlinx.android.synthetic.main.episode_result_downloaded.view.*
import kotlinx.android.synthetic.main.fragment_download_child.*
import kotlinx.android.synthetic.main.fragment_results.*
import kotlinx.android.synthetic.main.home_card.view.*
import kotlinx.android.synthetic.main.home_card.view.imageView
import java.io.File

class DownloadFragmentChild() : Fragment() {
    var anilistId: String? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isInResults = true
        arguments?.getString("anilist_id")?.let {
            anilistId = it
        }
        val topParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            LinearLayoutCompat.LayoutParams.MATCH_PARENT, // view width
            MainActivity.statusHeight // view height
        )
        top_padding_download_child.layoutParams = topParams

        println("ANILIST: " + anilistId)
        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        isInResults = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadData() {
        downloadRootChild.removeAllViews()
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(MainActivity.activity)
        val save = settingsManager.getBoolean("save_history", true)

        // When fastani is down it doesn't report any seasons and this is needed.
        val episodeKeys = DownloadFragment.childMetadataKeys[anilistId]
        val parent = DataStore.getKey<DownloadManager.DownloadParentFileMetadata>(DOWNLOAD_PARENT_KEY, anilistId!!)

        for (k in episodeKeys!!) {
            val child = DataStore.getKey<DownloadManager.DownloadFileMetadata>(k)

            if (child != null) {
                val file = File(child.videoPath)
                if (!file.exists()) {
                    continue
                }

                val card: View = layoutInflater.inflate(R.layout.episode_result_downloaded, null)
                if (child.thumbPath != null) {
                    card.imageView.setImageURI(Uri.parse(child.thumbPath))
                }

                val key = MainActivity.getViewKey(anilistId!!, child.seasonIndex, child.episodeIndex)

                card.cardRemoveIcon.setOnClickListener {
                    file.delete()
                    card.visibility = GONE
                    DataStore.removeKey(k)
                    Toast.makeText(
                        context,
                        "${child.videoTitle} S${child.seasonIndex + 1} E${child.episodeIndex + 1} deleted",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // CANT DOWNLOAD DOWNLOADED FILES
                /*
                card.cardRemoveIcon.visibility = View.GONE
                val param = card.cardTitle.layoutParams as ViewGroup.MarginLayoutParams
                param.updateMarginsRelative(
                    card.cardTitle.marginLeft,
                    card.cardTitle.marginTop,
                    10.toPx,
                    card.cardTitle.marginBottom
                )
                card.cardTitle.layoutParams = param*/


                card.imageView.setOnClickListener {
                    val castContext = CastContext.getSharedInstance(activity!!.applicationContext)
                    println("SSTATE: " + castContext.castState + "<<")
                    if (save) {
                        DataStore.setKey<Long>(VIEWSTATE_KEY, key, System.currentTimeMillis())
                    }
                    MainActivity.loadPlayer(
                        PlayerData(
                            child.videoTitle,
                            child.videoPath,
                            child.episodeIndex,
                            child.seasonIndex,
                            null,
                            null,
                            anilistId
                        )
                    )
                    //MainActivity.loadPlayer(epIndex, index, data)
                }

                card.setOnLongClickListener {
                    if (ResultFragment.isViewState) {
                        if (DataStore.containsKey(VIEWSTATE_KEY, key)) {
                            DataStore.removeKey(VIEWSTATE_KEY, key)
                        } else {
                            DataStore.setKey<Long>(VIEWSTATE_KEY, key, System.currentTimeMillis())
                        }
                        loadData()
                    }
                    return@setOnLongClickListener true
                }

                val title = fixEpTitle(
                    child.videoTitle, child.episodeIndex + 1, child.seasonIndex + 1,
                    parent?.isMovie == true
                )

                card.cardTitle.text = title
                val megaBytesTotal = DownloadManager.convertBytesToAny(child.maxFileSize, 0, 2.0).toInt()
                val localBytesTotal = DownloadManager.convertBytesToAny(file.length(), 0, 2.0).toInt()
                card.cardTitleExtra.text = "$localBytesTotal / $megaBytesTotal MB"

                if (DataStore.containsKey(VIEWSTATE_KEY, key)) {
                    card.cardBg.setCardBackgroundColor(
                        requireContext().getColorFromAttr(
                            R.attr.colorPrimaryDark
                        )
                    )
                }

                val pro = MainActivity.getViewPosDur(anilistId!!, child.seasonIndex, child.episodeIndex)
                if (pro.dur > 0 && pro.pos > 0) {
                    var progress: Int = (pro.pos * 100L / pro.dur).toInt()
                    if (progress < 5) {
                        progress = 5
                    } else if (progress > 95) {
                        progress = 100
                    }
                    card.video_progress.progress = progress
                } else {
                    card.video_progress.alpha = 0f
                }
                downloadRootChild.addView(card)
                downloadRootChild.invalidate()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_download_child, container, false)
    }


    companion object {
        fun newInstance(_anilistId: String) =
            DownloadFragmentChild().apply {
                arguments = Bundle().apply {
                    putString("anilist_id", _anilistId)
                }
            }
    }
}