package com.jamieadkins.gwent.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jamieadkins.gwent.R
import com.jamieadkins.gwent.settings.BasePreferenceActivity.Companion.EXTRA_PREFERENCE_LAYOUT
import com.jamieadkins.gwent.settings.BasePreferenceActivity.Companion.EXTRA_PREFERENCE_TITLE
import kotlinx.android.synthetic.main.appbar_layout.*
import kotlinx.android.synthetic.main.fragment_settings.*

class GwentFragment : Fragment(), GwentController.SettingsNavigationCallback {

    private val controller = GwentController()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            title = getString(R.string.gwent)
        }

        toolbar.setTitleTextAppearance(requireContext(), R.style.GwentTextAppearance)

        val layoutManager = LinearLayoutManager(recycler_view.context)
        recycler_view.layoutManager = layoutManager
        recycler_view.adapter = controller.adapter
        controller.listener = this
        controller.requestModelBuild()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller.listener = null
    }

    override fun onNewsClicked() {
        showChromeCustomTab("https://www.playgwent.com/news")
    }

    override fun onEsportsClicked() {
        showChromeCustomTab("https://masters.playgwent.com/")
    }

    override fun onForumsClicked() {
        showChromeCustomTab("https://forums.cdprojektred.com/forum/en/gwent")
    }

    override fun onRedditClicked() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/gwent/"))
        startActivity(intent)
    }

    override fun onDiscordClicked() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/playgwent"))
        startActivity(intent)
    }

    override fun onTwitchClicked() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitch.tv/directory/game/Gwent%3A%20The%20Witcher%20Card%20Game"))
        startActivity(intent)
    }

    override fun onYoutubeClicked() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/channel/UCKS8WtM7U144j4fAU4xqiCw"))
        startActivity(intent)
    }

    override fun onSettingsClicked() {
        context?.let {
            val intent = Intent(it, SettingsActivity::class.java)
            intent.putExtra(EXTRA_PREFERENCE_TITLE, R.string.settings)
            intent.putExtra(EXTRA_PREFERENCE_LAYOUT, R.xml.settings)
            startActivity(intent)
        }
    }

    override fun onAboutClicked() {
        context?.let {
            val intent = Intent(it, SettingsActivity::class.java)
            intent.putExtra(EXTRA_PREFERENCE_TITLE, R.string.about)
            intent.putExtra(EXTRA_PREFERENCE_LAYOUT, R.xml.about)
            startActivity(intent)
        }
    }

    private fun showChromeCustomTab(url: String) {
        context?.let {
            CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(it, R.color.gwentGreen))
                .setShowTitle(true)
                .build()
                .launchUrl(context, Uri.parse(url))
        }
    }
}
