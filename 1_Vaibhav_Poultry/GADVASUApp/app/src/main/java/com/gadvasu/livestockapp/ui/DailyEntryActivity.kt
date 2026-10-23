package com.gadvasu.livestockapp.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.Calc
import com.gadvasu.livestockapp.data.DailyRecord
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.util.Fmt
import com.gadvasu.livestockapp.util.confirm
import com.gadvasu.livestockapp.util.pickDate
import com.gadvasu.livestockapp.util.profitColor
import com.gadvasu.livestockapp.util.toast

/**
 * Shared behaviour for daily enterprises (Livestock, Layer):
 * pick a date -> load that day's record (edit) or a blank form -> live profit slip
 * -> save / update -> recent records list with delete.
 */
abstract class DailyEntryActivity : BaseActivity() {

    protected abstract val type: String
    protected var date: String = Fmt.today()

    // Views supplied by each screen
    protected abstract val scroller: ScrollView
    protected abstract val dateField: TextView
    protected abstract val noticeView: View
    protected abstract val errorView: TextView
    protected abstract val saveButton: MaterialButton
    protected abstract val resultTitle: TextView
    protected abstract val slipBox: LinearLayout
    protected abstract val recentBox: LinearLayout
    protected abstract val seeAllButton: View

    /** Fill the form from a saved record, or reset it when [existing] is null. */
    protected abstract fun fillForm(existing: DailyRecord?)

    /** Build a record from the form; show an error and return null if something is wrong. */
    protected abstract fun buildRecord(id: String): DailyRecord?

    protected abstract fun currentIncome(): Double
    protected abstract fun currentExpense(): Double

    private var liveInc: TextView? = null
    private var liveExp: TextView? = null
    private var liveLbl: TextView? = null
    private var livePr: TextView? = null

    /** Call at the end of onCreate, after the binding is set. */
    protected fun initDaily(savedInstanceState: Bundle?) {
        date = savedInstanceState?.getString(STATE_DATE) ?: Fmt.today()
        dateField.setOnClickListener { pickDate(date) { d -> loadDate(d) } }
        saveButton.setOnClickListener { save() }
        seeAllButton.setOnClickListener { startActivity(MainActivity.tabIntent(this, R.id.nav_reports)) }
        loadDate(date)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_DATE, date)
    }

    protected fun loadDate(d: String) {
        date = d
        dateField.text = Fmt.display(this, d)
        val existing = FarmRepository.recordFor(type, d)
        noticeView.isVisible = existing != null
        saveButton.setText(if (existing != null) R.string.update else R.string.save)
        errorView.text = ""
        resultTitle.text = getString(R.string.result_for, Fmt.display(this, d))
        buildSlip()
        fillForm(existing)
        refreshLive()
        renderRecent()
    }

    private fun buildSlip() {
        val s = Slip(this, slipBox)
        liveInc = s.row(getString(R.string.income), Fmt.money(0.0)).second
        liveExp = s.row(getString(R.string.expenses), Fmt.money(0.0)).second
        val g = s.grand(getString(R.string.profit), 0.0)
        liveLbl = g.first
        livePr = g.second
        s.foot(getString(R.string.daily_formula))
    }

    /** Update the result slip from what is typed right now. */
    protected fun refreshLive() {
        val pr0 = livePr ?: return
        val inc = currentIncome()
        val exp = currentExpense()
        val pr = inc - exp
        liveInc?.text = Fmt.money(inc)
        liveExp?.text = Fmt.money(exp)
        pr0.text = Fmt.money(pr)
        pr0.setTextColor(profitColor(pr))
        liveLbl?.setText(if (pr < 0) R.string.loss else R.string.profit)
    }

    private fun save() {
        errorView.text = ""
        val existing = FarmRepository.recordFor(type, date)
        val rec = buildRecord(existing?.id ?: FarmRepository.newId()) ?: return
        rec.date = date
        if (Calc.income(rec) == 0.0 && Calc.expense(rec) == 0.0 && rec.production == 0.0) {
            errorView.setText(R.string.enter_something)
            return
        }
        FarmRepository.upsertDaily(type, rec)
        toast(R.string.saved)
        loadDate(date)
    }

    protected open fun subtitle(r: DailyRecord): String =
        getString(R.string.income) + " " + Fmt.money(Calc.income(r)) + ", " +
            getString(R.string.expenses) + " " + Fmt.money(Calc.expense(r))

    private fun renderRecent() {
        recentBox.removeAllViews()
        val recs = FarmRepository.daily(type).sortedWith(Calc.byDateDesc)
        seeAllButton.isVisible = recs.size > 7
        if (recs.isEmpty()) {
            V.empty(this, recentBox, getString(R.string.no_records))
            return
        }
        val list = V.listCard(this, recentBox)
        for (r in recs.take(7)) {
            val pr = Calc.profit(r)
            V.recordRow(
                this, list, Fmt.display(this, r.date), subtitle(r), Fmt.money(pr), profitColor(pr),
                onClick = {
                    loadDate(r.date)
                    scroller.smoothScrollTo(0, 0)
                },
                onDelete = {
                    confirm(R.string.confirm_delete) {
                        FarmRepository.deleteDaily(type, r.id)
                        loadDate(date)
                    }
                }
            )
        }
    }

    companion object {
        private const val STATE_DATE = "date"
    }
}
