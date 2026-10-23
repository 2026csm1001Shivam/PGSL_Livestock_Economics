package com.gadvasu.livestockapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.Calc
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.FragmentDashboardBinding
import com.gadvasu.livestockapp.util.Fmt
import com.gadvasu.livestockapp.util.dp

/** Dashboard: today and this month for daily enterprises, batches, overall totals. */
class DashboardFragment : Fragment() {

    private var _b: FragmentDashboardBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentDashboardBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val ctx = requireContext()
        val box = b.content
        box.removeAllViews()
        val today = Fmt.today()
        V.h1(ctx, box, getString(R.string.dashboard))
        V.muted(ctx, box, Fmt.long(ctx, today))

        var hasAny = false
        val month = Fmt.today().substring(0, 8) + "01"

        // Daily enterprises
        val dailyTypes = Ent.DAILY.filter { FarmRepository.daily(it).isNotEmpty() }
        if (dailyTypes.isNotEmpty()) {
            hasAny = true
            V.h2(ctx, box, getString(R.string.daily_enterprises))
            dailyTypes.forEachIndexed { i, type ->
                val recs = FarmRepository.daily(type)
                val card = V.card(ctx, box, top = if (i > 0) 12 else 0)
                V.h3(ctx, card, getString(Ent.nameRes(type)), top = 0)
                val tr = recs.firstOrNull { it.date == today }
                if (tr != null) {
                    val unit = getString(if (type == Ent.LIVESTOCK) R.string.unit_litres else R.string.unit_eggs)
                    val line = if (tr.production > 0) getString(R.string.today_with_prod, Fmt.num(tr.production), unit)
                    else getString(R.string.today)
                    V.muted(ctx, card, line)
                    V.stat3(ctx, card, Calc.income(tr), Calc.expense(tr))
                } else {
                    V.muted(ctx, card, getString(R.string.no_entry_today))
                    val btn = layoutInflater.inflate(R.layout.view_button_secondary, card, false) as MaterialButton
                    btn.text = getString(R.string.add_today)
                    btn.setOnClickListener {
                        val target = if (type == Ent.LIVESTOCK) LivestockActivity::class.java else LayerActivity::class.java
                        startActivity(Intent(ctx, target))
                    }
                    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    lp.topMargin = ctx.dp(8)
                    card.addView(btn, lp)
                }
                V.separator(ctx, card)
                val m = Calc.summary(recs.filter { it.date in month..today })
                V.muted(ctx, card, getString(R.string.this_month) + ", " + m.count + " " +
                    getString(if (m.count == 1) R.string.record else R.string.records))
                V.stat3(ctx, card, m.income, m.expense)
            }
        }

        // Batch enterprises
        val all = FarmRepository.batches()
        val active = all.filter { !it.isClosed }.sortedByDescending { it.startDate }
        val done = all.filter { it.isClosed }.sortedByDescending { it.saleDate }
        if (active.isNotEmpty() || done.isNotEmpty()) {
            hasAny = true
            V.h2(ctx, box, getString(R.string.batch_enterprises))
            if (active.isNotEmpty()) {
                V.h3(ctx, box, getString(R.string.active_batches), top = 0)
                val list = V.listCard(ctx, box)
                active.forEach { V.batchRow(ctx, list, it, true) }
            }
            if (done.isNotEmpty()) {
                V.h3(ctx, box, getString(R.string.completed_batches))
                val list = V.listCard(ctx, box)
                done.take(5).forEach { V.batchRow(ctx, list, it, true) }
            }
        }

        if (hasAny) {
            var dInc = 0.0
            var dExp = 0.0
            Ent.DAILY.forEach { type ->
                val s = Calc.summary(FarmRepository.daily(type))
                dInc += s.income
                dExp += s.expense
            }
            val bInc = done.sumOf { it.amountReceived }
            val bExp = done.sumOf { Calc.batchExpense(it) }
            overallSlip(box, dInc, dExp, bInc, bExp, getString(R.string.overall_note))
        } else {
            V.empty(ctx, box, getString(R.string.no_data_yet), getString(R.string.go_home), {
                (activity as? MainActivity)?.selectTab(R.id.nav_home)
            }, top = 16)
        }
    }

    private fun overallSlip(box: LinearLayout, dInc: Double, dExp: Double, bInc: Double, bExp: Double, foot: String) {
        val ctx = requireContext()
        V.h2(ctx, box, getString(R.string.overall))
        val s = Slip(ctx, V.slipCard(ctx, box))
        val inc = dInc + bInc
        val exp = dExp + bExp
        val pr = inc - exp
        s.row(getString(R.string.daily_income), Fmt.money(dInc), sub = true)
        s.row(getString(R.string.batch_sales), Fmt.money(bInc), sub = true)
        s.total(getString(R.string.total_income), Fmt.money(inc))
        s.row(getString(R.string.daily_expenses), Fmt.money(dExp), sub = true)
        s.row(getString(R.string.batch_expenses), Fmt.money(bExp), sub = true)
        s.total(getString(R.string.total_expenses), Fmt.money(exp))
        s.grand(getString(if (pr < 0) R.string.net_loss else R.string.net_profit), pr)
        s.foot(foot)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
