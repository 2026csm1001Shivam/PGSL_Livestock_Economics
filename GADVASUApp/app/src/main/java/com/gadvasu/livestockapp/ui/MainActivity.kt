package com.gadvasu.livestockapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.ActivityMainBinding

/** Hosts the four bottom-navigation tabs: Home, Dashboard, Reports, Settings. */
class MainActivity : BaseActivity() {

    private lateinit var b: ActivityMainBinding
    private var currentTab = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireUser() == null) return
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupTopBar(b.topbar, getString(R.string.app_name), showBack = false)

        b.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId != currentTab) show(item.itemId)
            true
        }
        if (savedInstanceState == null) {
            selectTab(intent.getIntExtra(EXTRA_TAB, R.id.nav_home))
        } else {
            // The fragment itself is restored by the system; just restore title and tab
            currentTab = savedInstanceState.getInt(STATE_TAB, R.id.nav_home)
            updateTitle()
        }
    }

    /** Records downloaded from the cloud: redraw the open tab. */
    private val onCloudData: () -> Unit = {
        if (::b.isInitialized && !isFinishing) show(currentTab)
    }

    override fun onResume() {
        super.onResume()
        FarmRepository.addListener(onCloudData)
    }

    override fun onPause() {
        FarmRepository.removeListener(onCloudData)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tab = intent.getIntExtra(EXTRA_TAB, 0)
        if (tab != 0 && ::b.isInitialized) selectTab(tab)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TAB, currentTab)
    }

    fun selectTab(id: Int) {
        show(id)
        b.bottomNav.menu.findItem(id)?.isChecked = true
    }

    private fun show(id: Int) {
        currentTab = id
        val f: Fragment = when (id) {
            R.id.nav_dashboard -> DashboardFragment()
            R.id.nav_reports -> ReportsFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> HomeFragment()
        }
        supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
        updateTitle()
    }

    private fun updateTitle() {
        b.topbar.tvTitle.setText(
            when (currentTab) {
                R.id.nav_dashboard -> R.string.dashboard
                R.id.nav_reports -> R.string.reports
                R.id.nav_settings -> R.string.settings
                else -> R.string.app_name
            }
        )
    }

    companion object {
        private const val EXTRA_TAB = "tab"
        private const val STATE_TAB = "tab"

        fun tabIntent(ctx: Context, tab: Int): Intent =
            Intent(ctx, MainActivity::class.java)
                .putExtra(EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
