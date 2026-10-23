package com.gadvasu.livestockapp.util

import android.app.DatePickerDialog
import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.gadvasu.livestockapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

fun Context.color(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

/** Green for profit, red for loss, normal ink for zero. */
fun Context.profitColor(n: Double): Int = color(
    when {
        n > 0 -> R.color.green
        n < 0 -> R.color.red
        else -> R.color.ink
    }
)

fun Context.toast(@StringRes id: Int) {
    Toast.makeText(this, id, Toast.LENGTH_SHORT).show()
}

/** Yes/No dialog with a red "Delete" style confirm. */
fun Context.confirm(@StringRes message: Int, onYes: () -> Unit) {
    MaterialAlertDialogBuilder(this)
        .setMessage(message)
        .setPositiveButton(R.string.delete_) { _, _ -> onYes() }
        .setNegativeButton(R.string.cancel, null)
        .show()
}

/** Date picker limited to today or earlier (and not before [min], if given). */
fun Context.pickDate(current: String, min: String? = null, onPicked: (String) -> Unit) {
    val cal = Calendar.getInstance()
    Fmt.parse(current)?.let { cal.time = it }
    val dialog = DatePickerDialog(
        this,
        { _, y, m, d ->
            val c = Calendar.getInstance()
            c.set(y, m, d)
            onPicked(Fmt.toIso(c))
        },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
    )
    val now = System.currentTimeMillis()
    dialog.datePicker.maxDate = now
    val minDate = Fmt.parse(min)?.time
    if (minDate != null && minDate <= now) dialog.datePicker.minDate = minDate
    dialog.show()
}

/** Positive number typed in the field, or 0 when empty / invalid. */
fun EditText.amount(): Double {
    val v = text?.toString()?.trim()?.replace(",", "")?.toDoubleOrNull() ?: return 0.0
    return if (v.isFinite() && v > 0) v else 0.0
}

fun EditText.count(): Int {
    val v = text?.toString()?.trim()?.toIntOrNull() ?: return 0
    return if (v > 0) v else 0
}

fun plainNumber(v: Double): String =
    if (v == floor(v) && abs(v) < 1e15) v.toLong().toString() else v.toString()

fun EditText.setAmount(v: Double) {
    setText(if (v == 0.0) "" else plainNumber(v))
}

fun EditText.setCount(v: Int) {
    setText(if (v == 0) "" else v.toString())
}
