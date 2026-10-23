package com.gadvasu.livestockapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.data.UserData
import com.gadvasu.livestockapp.databinding.ViewTopbarBinding

/** Shared top bar setup and the "must be logged in" check. */
abstract class BaseActivity : AppCompatActivity() {

    /** Returns the farmer, or sends them to Login and returns null. */
    protected fun requireUser(): UserData? {
        val u = FarmRepository.user()
        if (u == null) {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }
        return u
    }

    protected fun setupTopBar(bar: ViewTopbarBinding, title: String, showBack: Boolean = true) {
        bar.tvTitle.text = title
        bar.btnBack.isVisible = showBack
        bar.ivBrand.isVisible = !showBack
        bar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        bar.btnLang.text = langShort()
        bar.btnLang.setOnClickListener { startActivity(LanguageActivity.changeIntent(this)) }
    }

    private fun langShort(): String = when (FarmRepository.language) {
        "hi" -> "हिं"
        "pa" -> "ਪੰ"
        else -> "EN"
    }
}
