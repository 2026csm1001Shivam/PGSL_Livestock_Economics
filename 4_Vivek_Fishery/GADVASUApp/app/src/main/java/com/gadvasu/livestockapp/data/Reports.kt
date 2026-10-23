package com.gadvasu.livestockapp.data

import android.content.Context
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.util.Fmt
import com.gadvasu.livestockapp.util.plainNumber

/** Report totals by enterprise and period, plus CSV export (same rules as the prototype). */
object Reports {

    const val ALL = "all"
    const val P7 = "7"
    const val P30 = "30"
    const val MONTH = "month"
    const val ALL_TIME = "all"
    const val CUSTOM = "custom"
    const val MIN_DATE = "0000-01-01"
    const val MAX_DATE = "9999-12-31"

    data class DailyRow(val type: String, val r: DailyRecord)

    class Data(
        val dailyRows: List<DailyRow>,
        val daily: Calc.Summary,
        val sold: List<Batch>,
        val running: List<Batch>,
        val batchInc: Double,
        val batchExp: Double
    ) {
        val income: Double get() = daily.income + batchInc
        val expense: Double get() = daily.expense + batchExp
        val isEmpty: Boolean get() = dailyRows.isEmpty() && sold.isEmpty() && running.isEmpty()
    }

    fun range(period: String, from: String, to: String): Pair<String, String> {
        val today = Fmt.today()
        return when (period) {
            P7 -> Pair(Fmt.shift(today, -6), today)
            P30 -> Pair(Fmt.shift(today, -29), today)
            MONTH -> Pair(today.substring(0, 8) + "01", today)
            CUSTOM -> Pair(from.ifEmpty { MIN_DATE }, to.ifEmpty { MAX_DATE })
            else -> Pair(MIN_DATE, MAX_DATE)
        }
    }

    fun data(ent: String, range: Pair<String, String>): Data {
        val types = if (ent == ALL) Ent.ORDER else listOf(ent)
        val rows = mutableListOf<DailyRow>()
        for (type in types) {
            if (!Ent.isDaily(type)) continue
            for (r in FarmRepository.daily(type)) {
                if (r.date >= range.first && r.date <= range.second) rows.add(DailyRow(type, r))
            }
        }
        rows.sortWith(compareByDescending<DailyRow> { it.r.date }.thenByDescending { it.r.id })
        val summary = Calc.summary(rows.map { it.r })

        val sold = mutableListOf<Batch>()
        val running = mutableListOf<Batch>()
        var bInc = 0.0
        var bExp = 0.0
        for (b in FarmRepository.batches()) {
            if (b.type !in types) continue
            if (b.isClosed) {
                if (b.saleDate >= range.first && b.saleDate <= range.second) {
                    sold.add(b)
                    bInc += b.amountReceived
                    bExp += Calc.batchExpense(b)
                }
            } else {
                running.add(b)
            }
        }
        sold.sortByDescending { it.saleDate }
        return Data(rows, summary, sold, running, bInc, bExp)
    }

    private fun cell(v: Any?): String {
        val s = v?.toString() ?: ""
        return if (s.contains(',') || s.contains('"') || s.contains('\n') || s.contains('\r')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
    }

    private fun n(v: Double): String = plainNumber(Math.round(v * 100.0) / 100.0)

    /** CSV text (opens in Excel). Empty string when there is nothing to export. */
    fun csv(ctx: Context, d: Data): String {
        val rows = mutableListOf(
            listOf("Enterprise", "Record type", "Date", "Batch name", "Income (INR)", "Expenses (INR)", "Profit (INR)", "Details")
        )
        for (x in d.dailyRows) {
            val r = x.r
            val parts = mutableListOf<String>()
            val ls = r.livestock
            if (ls != null) {
                if (ls.cows > 0) parts.add(ctx.getString(R.string.cows_count) + " " + ls.cows)
                if (ls.buffaloes > 0) parts.add(ctx.getString(R.string.buffaloes_count) + " " + ls.buffaloes)
                if (ls.cowLitres > 0) parts.add(ctx.getString(R.string.cow_milk) + " " + n(ls.cowLitres) + " L @ " + n(ls.cowRate))
                if (ls.buffaloLitres > 0) parts.add(ctx.getString(R.string.buffalo_milk) + " " + n(ls.buffaloLitres) + " L @ " + n(ls.buffaloRate))
                ls.feeds.forEach { parts.add(ctx.getString(R.string.feed) + ": " + it.name + " " + n(it.price)) }
            } else if (r.production > 0) {
                parts.add(ctx.getString(R.string.eggs) + " " + n(r.production))
            }
            for ((k, v) in r.income) if (v != 0.0) parts.add(ctx.getString(Ent.incomeLabel(k)) + " " + n(v))
            for ((k, v) in r.expenses) if (v != 0.0 && !(ls != null && k == "feed")) parts.add(ctx.getString(Ent.expenseLabel(k)) + " " + n(v))
            if (r.note.isNotEmpty()) parts.add(r.note)
            rows.add(listOf(ctx.getString(Ent.nameRes(x.type)), "Daily", r.date, "",
                n(Calc.income(r)), n(Calc.expense(r)), n(Calc.profit(r)), parts.joinToString("; ")))
        }
        fun batchParts(b: Batch): String {
            val cfg = Ent.batch(b.type)
            val parts = mutableListOf(
                ctx.getString(R.string.started) + " " + b.startDate,
                b.count.toString() + " " + ctx.getString(cfg.unitPlRes),
                Calc.batchDays(b).toString() + " " + ctx.getString(R.string.days)
            )
            for ((k, v) in Calc.batchByCat(b)) {
                val label = if (k == "purchase") ctx.getString(cfg.costLabelRes) else ctx.getString(Ent.expenseLabel(k))
                parts.add(label + " " + n(v))
            }
            return parts.joinToString("; ")
        }
        for (b in d.sold) {
            rows.add(listOf(ctx.getString(Ent.nameRes(b.type)), "Batch (sold)", b.saleDate, b.name,
                n(b.amountReceived), n(Calc.batchExpense(b)), n(Calc.batchProfit(b) ?: 0.0), batchParts(b)))
        }
        for (b in d.running) {
            rows.add(listOf(ctx.getString(Ent.nameRes(b.type)), "Batch (running)", b.startDate, b.name,
                "", n(Calc.batchExpense(b)), "", batchParts(b)))
        }
        if (rows.size <= 1) return ""
        return rows.joinToString("\r\n") { row -> row.joinToString(",") { cell(it) } }
    }
}
