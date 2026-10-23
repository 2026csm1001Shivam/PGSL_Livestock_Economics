package com.gadvasu.livestockapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.ActivityLanguageBinding

/**
 * Launcher screen. On first launch the farmer picks English / Hindi / Punjabi;
 * afterwards it forwards straight to Login or Home. Also opened from the
 * language chip in the top bar to change language.
 */
class LanguageActivity : BaseActivity() {

    private lateinit var b: ActivityLanguageBinding
    private val changeMode: Boolean get() = intent.getBooleanExtra(EXTRA_CHANGE, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!changeMode && FarmRepository.language != null) {
            openNext()
            return
        }
        b = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(b.root)

        val current = FarmRepository.language
        b.tvTitle.text = getString(if (current == null) R.string.select_language_en else R.string.select_language)
        b.tvPrompt.isVisible = current == null
        bind(b.btnEn, "en", current)
        bind(b.btnHi, "hi", current)
        bind(b.btnPa, "pa", current)
    }

    private fun bind(v: View, code: String, current: String?) {
        v.isSelected = code == current
        v.setOnClickListener { choose(code) }
    }

    private fun choose(code: String) {
        FarmRepository.language = code
        // Applies the language to the whole app (and remembers it on Android 12 and below)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
        if (changeMode) finish() else openNext()
    }

    private fun openNext() {
        val next = if (FarmRepository.user() == null) LoginActivity::class.java else MainActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }

    companion object {
        private const val EXTRA_CHANGE = "change"

        fun changeIntent(ctx: Context): Intent =
            Intent(ctx, LanguageActivity::class.java).putExtra(EXTRA_CHANGE, true)
    }
}
