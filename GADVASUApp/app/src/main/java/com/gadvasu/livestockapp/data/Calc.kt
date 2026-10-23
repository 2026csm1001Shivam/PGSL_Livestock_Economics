package com.gadvasu.livestockapp.data

import com.gadvasu.livestockapp.util.Fmt
import kotlin.math.max

/**
 * Calculation engine (same formulas as the prototype):
 *   Daily profit = daily income - daily expenses
 *   Batch profit = amount received - (purchase cost + all expenses in the batch)
 */
object Calc {

    data class Summary(val income: Double, val expense: Double, val count: Int) {
        val profit: Double get() = income - expense
    }

    val byDateDesc: Comparator<DailyRecord> =
        compareByDescending<DailyRecord> { it.date }.thenByDescending { it.id }

    fun sum(m: Map<String, Double>): Double = m.values.sumOf { if (it.isFinite()) it else 0.0 }

    fun income(r: DailyRecord): Double = sum(r.income)
    fun expense(r: DailyRecord): Double = sum(r.expenses)
    fun profit(r: DailyRecord): Double = income(r) - expense(r)

    fun summary(recs: List<DailyRecord>): Summary =
        Summary(recs.sumOf { income(it) }, recs.sumOf { expense(it) }, recs.size)

    /** Batch expenses grouped by category; "purchase" is the starting purchase cost. */
    fun batchByCat(b: Batch): LinkedHashMap<String, Double> {
        val out = LinkedHashMap<String, Double>()
        if (b.purchaseCost > 0) out["purchase"] = b.purchaseCost
        for (r in b.records) {
            for ((k, v) in r.expenses) {
                if (v != 0.0 && v.isFinite()) out[k] = (out[k] ?: 0.0) + v
            }
        }
        return out
    }

    fun batchExpense(b: Batch): Double = sum(batchByCat(b))

    fun batchProfit(b: Batch): Double? = if (b.isClosed) b.amountReceived - batchExpense(b) else null

    fun batchDays(b: Batch): Int {
        val end = if (b.isClosed && b.saleDate.isNotEmpty()) b.saleDate else Fmt.today()
        return max(0, Fmt.daysBetween(b.startDate, end))
    }
}
