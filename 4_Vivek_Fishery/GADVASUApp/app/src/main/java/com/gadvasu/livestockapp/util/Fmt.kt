package com.gadvasu.livestockapp.util

import android.content.Context
import androidx.core.os.ConfigurationCompat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Money (₹1,32,000 Indian grouping) and date helpers. Dates are stored as "yyyy-MM-dd". */
object Fmt {

    private val inr: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    private fun iso(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    private fun locale(ctx: Context): Locale =
        ConfigurationCompat.getLocales(ctx.resources.configuration)[0] ?: Locale.getDefault()

    fun money(n: Double): String {
        val r = Math.round(n * 100.0) / 100.0
        val s = "₹" + inr.format(abs(r))
        return if (r < 0) "−$s" else s
    }

    fun num(n: Double): String = inr.format(n)
    fun num(n: Int): String = inr.format(n.toLong())

    fun today(): String = iso().format(Date())

    fun toIso(cal: Calendar): String = iso().format(cal.time)

    fun parse(s: String?): Date? {
        if (s.isNullOrBlank()) return null
        return try {
            iso().parse(s)
        } catch (e: Exception) {
            null
        }
    }

    fun shift(s: String, days: Int): String {
        val c = Calendar.getInstance()
        c.time = parse(s) ?: Date()
        c.add(Calendar.DAY_OF_MONTH, days)
        return iso().format(c.time)
    }

    fun daysBetween(a: String, b: String): Int {
        val da = parse(a) ?: return 0
        val db = parse(b) ?: return 0
        return ((db.time - da.time) / 86_400_000.0).roundToLong().toInt()
    }

    /** "7 Oct 2026" in the app language. */
    fun display(ctx: Context, s: String?): String {
        val d = parse(s) ?: return s ?: ""
        return SimpleDateFormat("d MMM yyyy", locale(ctx)).format(d)
    }

    /** "Saturday, 24 October 2026" in the app language. */
    fun long(ctx: Context, s: String): String {
        val d = parse(s) ?: return s
        return SimpleDateFormat("EEEE, d MMMM yyyy", locale(ctx)).format(d)
    }
}
