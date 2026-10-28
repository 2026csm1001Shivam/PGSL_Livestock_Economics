package com.gadvasu.livestockapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.FragmentSettingsBinding
import com.gadvasu.livestockapp.util.confirm
import com.gadvasu.livestockapp.util.toast

/** Settings: language, account, data, about. */
class SettingsFragment : Fragment() {

    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentSettingsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        b.tvLanguageName.setText(
            when (FarmRepository.language) {
                "hi" -> R.string.lang_hi_native
                "pa" -> R.string.lang_pa_native
                else -> R.string.lang_en_native
            }
        )
        val p = FarmRepository.user()?.profile
        val who = if (p == null) "" else p.value + if (p.name.isNotBlank()) " (" + p.name + ")" else ""
        b.tvLoggedIn.text = getString(R.string.logged_in_as, who)
        b.tvVersion.text = getString(R.string.version, "2.0")

        b.btnChangeLanguage.setOnClickListener { startActivity(LanguageActivity.changeIntent(ctx)) }
        b.btnLogout.setOnClickListener {
            FarmRepository.logout()
            startActivity(
                Intent(ctx, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        b.btnClearData.setOnClickListener {
            ctx.confirm(R.string.confirm_clear) {
                FarmRepository.clearAll()
                ctx.toast(R.string.cleared)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
