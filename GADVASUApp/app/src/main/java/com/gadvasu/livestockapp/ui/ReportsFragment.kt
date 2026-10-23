package com.gadvasu.livestockapp.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.Calc
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.data.Reports
import com.gadvasu.livestockapp.databinding.FragmentReportsBinding
import com.gadvasu.livestockapp.util.Fmt
import com.gadvasu.livestockapp.util.confirm
import com.gadvasu.livestockapp.util.pickDate
import com.gadvasu.livestockapp.util.profitColor
import com.gadvasu.livestockapp.util.toast

/** Reports: choose enterprise and period, see totals and records, export CSV. */
class ReportsFragment : Fragment() {

    private var _b: FragmentReportsBinding? = null
    private val b get() = _b!!

    private var ent = Reports.ALL
    private var period = Reports.P30
    private var from = ""
    private var to = ""
    private var pendingCsv: String? = null

    private val entKeys = listOf(Reports.ALL) + Ent.ORDER
    private val periodKeys = listOf(Reports.P7, Reports.P30, Reports.MONTH, Reports.ALL_TIME, Reports.CUSTOM)

    /** System "save file" screen; the farmer picks where to put the CSV (e.g. Downloads). */
    private val saveCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        val csv = pendingCsv
        pendingCsv = null
        if (uri == null || csv == null) return@registerForActivityResult
        val ctx = context ?: return@registerForActivityResult
        try {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(("﻿" + csv).toByteArray(Charsets.UTF_8)) }
            ctx.toast(R.string.saved)
        } catch (e: Exception) {
            ctx.toast(R.string.nothing_to_export)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentReportsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            ent = it.getString("ent", Reports.ALL)
            period = it.getString("period", Reports.P30)
            from = it.getString("from", "")
            to = it.getString("to", "")
        }
        val entLabels = listOf(getString(R.string.all)) + Ent.ORDER.map { getString(Ent.nameRes(it)) }
        val periodLabels = listOf(
            getString(R.string.last7), getString(R.string.last30), getString(R.string.this_month),
            getString(R.string.all_time), getString(R.string.custom)
        )
        setupSpinner(b.spEnt, entLabels, entKeys.indexOf(ent)) { i -> ent = entKeys[i] }
        setupSpinner(b.spPeriod, periodLabels, periodKeys.indexOf(period)) { i -> period = periodKeys[i] }

        b.tvFrom.setOnClickListener {
            requireContext().pickDate(from.ifEmpty { Fmt.today() }) { d -> from = d; render() }
        }
        b.tvTo.setOnClickListener {
            requireContext().pickDate(to.ifEmpty { Fmt.today() }, from.ifEmpty { null }) { d -> to = d; render() }
        }
        b.btnExport.setOnClickListener { export() }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("ent", ent)
        outState.putString("period", period)
        outState.putString("from", from)
        outState.putString("to", to)
    }

    private fun setupSpinner(sp: Spinner, labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, labels)
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        sp.adapter = adapter
        sp.setSelection(if (selected >= 0) selected else 0, false)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var last = sp.selectedItemPosition
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == last) return
                last = position
                onPick(position)
                render()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun render() {
        val binding = _b ?: return
        val ctx = requireContext()
        binding.customGroup.isVisible = period == Reports.CUSTOM
        binding.tvFrom.text = if (from.isEmpty()) "" else Fmt.display(ctx, from)
        binding.tvTo.text = if (to.isEmpty()) "" else Fmt.display(ctx, to)

        val range = Reports.range(period, from, to)
        val d = Reports.data(ent, range)
        val box = binding.resultsBox
        box.removeAllViews()

        val heading = if (period == Reports.ALL_TIME || (period == Reports.CUSTOM && from.isEmpty() && to.isEmpty())) {
            getString(R.string.all_time)
        } else {
            val a = if (range.first == Reports.MIN_DATE) "…" else Fmt.display(ctx, range.first)
            val z = if (range.second == Reports.MAX_DATE) "…" else Fmt.display(ctx, range.second)
            "$a – $z"
        }
        V.h2(ctx, box, heading)
        val s = Slip(ctx, V.slipCard(ctx, box))
        s.row(getString(R.string.daily_income), Fmt.money(d.daily.income), sub = true)
        s.row(getString(R.string.batch_sales), Fmt.money(d.batchInc), sub = true)
        s.total(getString(R.string.total_income), Fmt.money(d.income))
        s.row(getString(R.string.daily_expenses), Fmt.money(d.daily.expense), sub = true)
        s.row(getString(R.string.batch_expenses), Fmt.money(d.batchExp), sub = true)
        s.total(getString(R.string.total_expenses), Fmt.money(d.expense))
        val pr = d.income - d.expense
        s.grand(getString(if (pr < 0) R.string.net_loss else R.string.net_profit), pr)

        if (d.dailyRows.isNotEmpty()) {
            V.h2(ctx, box, getString(R.string.daily_records) + " (" + d.dailyRows.size + ")")
            val list = V.listCard(ctx, box)
            for (x in d.dailyRows) {
                val r = x.r
                val p = Calc.profit(r)
                V.recordRow(
                    ctx, list, Fmt.display(ctx, r.date),
                    getString(R.string.income) + " " + Fmt.money(Calc.income(r)) + ", " +
                        getString(R.string.expenses) + " " + Fmt.money(Calc.expense(r)),
                    Fmt.money(p), ctx.profitColor(p),
                    tag = if (ent == Reports.ALL) getString(Ent.nameRes(x.type)) else null,
                    onDelete = {
                        ctx.confirm(R.string.confirm_delete) {
                            FarmRepository.deleteDaily(x.type, r.id)
                            render()
                        }
                    }
                )
            }
        }
        if (d.sold.isNotEmpty()) {
            V.h2(ctx, box, getString(R.string.sold_in_period) + " (" + d.sold.size + ")")
            val list = V.listCard(ctx, box)
            d.sold.forEach { V.batchRow(ctx, list, it, true) }
        }
        if (d.running.isNotEmpty()) {
            V.h2(ctx, box, getString(R.string.running_not_included))
            val list = V.listCard(ctx, box)
            d.running.forEach { V.batchRow(ctx, list, it, true) }
        }
        if (d.isEmpty) V.empty(ctx, box, getString(R.string.no_records_period), top = 16)
    }

    private fun export() {
        val ctx = requireContext()
        val csv = Reports.csv(ctx, Reports.data(ent, Reports.range(period, from, to)))
        if (csv.isEmpty()) {
            ctx.toast(R.string.nothing_to_export)
            return
        }
        pendingCsv = csv
        saveCsv.launch("gadvasu-report-" + Fmt.today() + ".csv")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
