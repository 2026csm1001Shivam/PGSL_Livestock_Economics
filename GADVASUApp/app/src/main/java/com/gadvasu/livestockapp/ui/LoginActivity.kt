package com.gadvasu.livestockapp.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.ActivityLoginBinding

/**
 * Login with a mobile number or email ID (validated like the prototype).
 * Frontend only: no OTP yet. Firebase Auth will replace FarmRepository.login().
 */
class LoginActivity : BaseActivity() {

    private lateinit var b: ActivityLoginBinding
    private var method = METHOD_MOBILE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupTopBar(b.topbar, getString(R.string.app_name), showBack = false)

        method = savedInstanceState?.getString(STATE_METHOD) ?: METHOD_MOBILE
        b.toggle.check(if (method == METHOD_MOBILE) R.id.btnMobile else R.id.btnEmail)
        applyMethod()
        b.toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val m = if (checkedId == R.id.btnMobile) METHOD_MOBILE else METHOD_EMAIL
                if (m != method) {
                    method = m
                    b.etLogin.setText("")
                    applyMethod()
                }
            }
        }
        b.etLogin.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }
        b.btnContinue.setOnClickListener { submit() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_METHOD, method)
    }

    private fun applyMethod() {
        b.tvError.text = ""
        if (method == METHOD_MOBILE) {
            b.tvLoginLabel.setText(R.string.mobile)
            b.etLogin.inputType = InputType.TYPE_CLASS_PHONE
            b.etLogin.setHint(R.string.hint_mobile)
        } else {
            b.tvLoginLabel.setText(R.string.email)
            b.etLogin.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            b.etLogin.setHint(R.string.hint_email)
        }
    }

    private fun submit() {
        val raw = b.etLogin.text.toString().trim()
        val value: String
        if (method == METHOD_MOBILE) {
            var d = raw.filter { it in '0'..'9' }
            if (d.length == 12 && d.startsWith("91")) d = d.substring(2)
            if (d.length == 11 && d.startsWith("0")) d = d.substring(1)
            if (!MOBILE.matches(d)) {
                b.tvError.setText(R.string.login_invalid_mobile)
                return
            }
            value = d
        } else {
            value = raw.lowercase()
            if (!EMAIL.matches(value)) {
                b.tvError.setText(R.string.login_invalid_email)
                return
            }
        }
        FarmRepository.login(method, value, b.etName.text.toString().trim())
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    companion object {
        private const val METHOD_MOBILE = "mobile"
        private const val METHOD_EMAIL = "email"
        private const val STATE_METHOD = "method"
        private val MOBILE = Regex("^[6-9]\\d{9}$")
        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
