package com.gadvasu.livestockapp.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.ActivityLoginBinding
import com.gadvasu.livestockapp.util.toast
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * Login with Firebase Authentication:
 *  - Mobile: enter number -> Send OTP -> enter the 6-digit code -> Verify
 *    (on many phones the code is read automatically).
 *  - Email: email + password -> Login, or Create account for a new farmer; Forgot password sends a reset link.
 * After signing in, the farmer's records are downloaded from Firestore, then Home opens.
 */
class LoginActivity : BaseActivity() {

    private lateinit var b: ActivityLoginBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var method = DEFAULT_METHOD
    private var phone = ""
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupTopBar(b.topbar, getString(R.string.app_name), showBack = false)

        method = savedInstanceState?.getString(STATE_METHOD) ?: DEFAULT_METHOD
        phone = savedInstanceState?.getString(STATE_PHONE) ?: ""
        verificationId = savedInstanceState?.getString(STATE_VERIFICATION)

        b.toggle.check(if (method == METHOD_MOBILE) R.id.btnMobile else R.id.btnEmail)
        applyMethod()
        b.toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val m = if (checkedId == R.id.btnMobile) METHOD_MOBILE else METHOD_EMAIL
                if (m != method) {
                    method = m
                    verificationId = null
                    b.etLogin.setText("")
                    applyMethod()
                }
            }
        }
        b.etLogin.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }
        b.etOtp.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }
        b.btnContinue.setOnClickListener { submit() }
        b.btnCreateAccount.setOnClickListener { emailLogin(create = true) }
        b.btnForgot.setOnClickListener { resetPassword() }
        b.btnResend.setOnClickListener { sendOtp(resend = true) }
        b.btnChangeNumber.setOnClickListener {
            verificationId = null
            resendToken = null
            b.etOtp.setText("")
            applyMethod()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_METHOD, method)
        outState.putString(STATE_PHONE, phone)
        outState.putString(STATE_VERIFICATION, verificationId)
    }

    /** Shows the fields for mobile (number, then OTP) or email (email + password). */
    private fun applyMethod() {
        b.tvError.text = ""
        val mobile = method == METHOD_MOBILE
        val waitingForOtp = mobile && verificationId != null
        if (mobile) {
            b.tvLoginLabel.setText(R.string.mobile)
            b.etLogin.inputType = InputType.TYPE_CLASS_PHONE
            b.etLogin.setHint(R.string.hint_mobile)
        } else {
            b.tvLoginLabel.setText(R.string.email)
            b.etLogin.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            b.etLogin.setHint(R.string.hint_email)
        }
        b.etLogin.isEnabled = !waitingForOtp
        b.passwordGroup.isVisible = !mobile
        b.emailActions.isVisible = !mobile
        b.otpGroup.isVisible = waitingForOtp
        b.otpActions.isVisible = waitingForOtp
        if (waitingForOtp) b.tvOtpInfo.text = getString(R.string.otp_sent, "+91 $phone")
        b.btnContinue.setText(
            when {
                waitingForOtp -> R.string.verify_otp
                mobile -> R.string.send_otp
                else -> R.string.login
            }
        )
    }

    private fun setBusy(busy: Boolean, message: Int = R.string.loading_records) {
        b.busyGroup.isVisible = busy
        if (busy) b.tvBusy.setText(message)
        listOf(b.btnContinue, b.btnCreateAccount, b.btnForgot, b.btnResend, b.btnChangeNumber, b.btnMobile, b.btnEmail)
            .forEach { it.isEnabled = !busy }
    }

    private fun submit() {
        when {
            method == METHOD_MOBILE && verificationId == null -> sendOtp(resend = false)
            method == METHOD_MOBILE -> verifyOtp()
            else -> emailLogin(create = false)
        }
    }

    // ---------------- Mobile + OTP ----------------

    /** 10-digit Indian mobile number from what was typed (accepts +91 / 0 prefixes), or null. */
    private fun typedMobile(): String? {
        var d = b.etLogin.text.toString().filter { it in '0'..'9' }
        if (d.length == 12 && d.startsWith("91")) d = d.substring(2)
        if (d.length == 11 && d.startsWith("0")) d = d.substring(1)
        return if (MOBILE.matches(d)) d else null
    }

    private fun sendOtp(resend: Boolean) {
        b.tvError.text = ""
        if (!resend) {
            phone = typedMobile() ?: run {
                b.tvError.setText(R.string.login_invalid_mobile)
                return
            }
        }
        setBusy(true, R.string.send_otp)
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(otpCallbacks)
        val token = resendToken
        if (resend && token != null) builder.setForceResendingToken(token)
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    private val otpCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        // Android read the SMS by itself: sign in straight away
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWith(auth.signInWithCredential(credential), METHOD_MOBILE, phone)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            setBusy(false)
            showError(e)
        }

        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
            setBusy(false)
            verificationId = id
            resendToken = token
            applyMethod()
            b.etOtp.requestFocus()
        }
    }

    private fun verifyOtp() {
        val id = verificationId ?: return
        val code = b.etOtp.text.toString().trim()
        if (code.length != 6) {
            b.tvError.setText(R.string.enter_otp)
            return
        }
        signInWith(auth.signInWithCredential(PhoneAuthProvider.getCredential(id, code)), METHOD_MOBILE, phone)
    }

    // ---------------- Email + password ----------------

    private fun typedEmail(): String? {
        val e = b.etLogin.text.toString().trim().lowercase()
        return if (EMAIL.matches(e)) e else null
    }

    private fun emailLogin(create: Boolean) {
        b.tvError.text = ""
        val email = typedEmail() ?: run {
            b.tvError.setText(R.string.login_invalid_email)
            return
        }
        val password = b.etPassword.text.toString()
        if (password.length < 6) {
            b.tvError.setText(R.string.password_short)
            return
        }
        val task = if (create) auth.createUserWithEmailAndPassword(email, password)
        else auth.signInWithEmailAndPassword(email, password)
        signInWith(task, METHOD_EMAIL, email)
    }

    private fun resetPassword() {
        b.tvError.text = ""
        val email = typedEmail() ?: run {
            b.tvError.setText(R.string.login_invalid_email)
            return
        }
        setBusy(true, R.string.forgot_password)
        auth.sendPasswordResetEmail(email).addOnCompleteListener(this) { t ->
            setBusy(false)
            if (t.isSuccessful) toast(R.string.reset_sent) else showError(t.exception)
        }
    }

    // ---------------- Finish ----------------

    private fun signInWith(task: Task<AuthResult>, loginMethod: String, value: String) {
        b.tvError.text = ""
        setBusy(true)
        task.addOnSuccessListener(this) { finishLogin(loginMethod, value) }
            .addOnFailureListener(this) { e ->
                setBusy(false)
                showError(e)
            }
    }

    /** Signed in: open this farmer's data, download it from the cloud (max 10 s), then go Home. */
    private fun finishLogin(loginMethod: String, value: String) {
        setBusy(true, R.string.loading_records)
        FarmRepository.signIn(loginMethod, value, b.etName.text.toString().trim())
        lifecycleScope.launch {
            withTimeoutOrNull(10_000) { FarmRepository.syncFromCloud() }
            startActivity(
                Intent(this@LoginActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }
    }

    private fun showError(e: Exception?) {
        val waitingForOtp = method == METHOD_MOBILE && verificationId != null
        val msg = when (e) {
            is FirebaseNetworkException -> R.string.network_error
            is FirebaseTooManyRequestsException -> R.string.too_many_requests
            is FirebaseAuthUserCollisionException -> R.string.email_in_use
            is FirebaseAuthWeakPasswordException -> R.string.password_short
            is FirebaseAuthInvalidUserException -> R.string.login_failed
            is FirebaseAuthInvalidCredentialsException -> when {
                waitingForOtp -> R.string.wrong_otp
                method == METHOD_MOBILE -> R.string.otp_failed
                else -> R.string.login_failed
            }
            else -> if (method == METHOD_MOBILE && !waitingForOtp) R.string.otp_failed else R.string.something_wrong
        }
        // Show Firebase's own reason too, so setup problems (billing, SHA-1, disabled provider) are visible
        Log.w(TAG, "Login failed", e)
        val detail = e?.message?.takeIf { it.isNotBlank() }
        b.tvError.text = if (detail != null) getString(msg) + "\n(" + detail + ")" else getString(msg)
    }

    companion object {
        private const val TAG = "LoginActivity"
        private const val METHOD_MOBILE = "mobile"
        private const val METHOD_EMAIL = "email"
        // Email until Phone sign-in is enabled in Firebase; switch back to METHOD_MOBILE then
        private const val DEFAULT_METHOD = METHOD_EMAIL
        private const val STATE_METHOD = "method"
        private const val STATE_PHONE = "phone"
        private const val STATE_VERIFICATION = "verificationId"
        private val MOBILE = Regex("^[6-9]\\d{9}$")
        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
