package com.gadvasu.livestockapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.Batch
import com.gadvasu.livestockapp.data.BatchConfig
import com.gadvasu.livestockapp.data.BatchExpense
import com.gadvasu.livestockapp.data.Calc
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.ActivityBatchDetailBinding
import com.gadvasu.livestockapp.databinding.ViewMoneyFieldBinding
import com.gadvasu.livestockapp.util.Fmt
import com.gadvasu.livestockapp.util.amount
import com.gadvasu.livestockapp.util.color
import com.gadvasu.livestockapp.util.confirm
import com.gadvasu.livestockapp.util.dp
import com.gadvasu.livestockapp.util.pickDate
import com.gadvasu.livestockapp.util.profitColor
import com.gadvasu.livestockapp.util.toast

/** One batch: expense slip, add expenses while running, close when sold, expense records. */
class BatchDetailActivity : BaseActivity() {

    private lateinit var b: ActivityBatchDetailBinding
    private lateinit var batchId: String
    private val expFields = LinkedHashMap<String, EditText>()
    private var expDate = Fmt.today()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireUser() == null) return
        val id = intent.getStringExtra(EXTRA_ID)
        val batch = if (id == null) null else FarmRepository.batch(id)
        if (id == null || batch == null) {
            finish()
            return
        }
        batchId = id
        b = ActivityBatchDetailBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupTopBar(b.topbar, batch.name)
        buildExpenseFields(Ent.batch(batch.type))

        expDate = savedInstanceState?.getString(STATE_DATE) ?: Fmt.today()
        b.tvExpDate.setOnClickListener {
            val start = FarmRepository.batch(batchId)?.startDate
            pickDate(expDate, start) { d ->
                expDate = d
                b.tvExpDate.text = Fmt.display(this, d)
            }
        }
        b.btnSaveExpense.setOnClickListener { saveExpense() }
        b.btnClose.setOnClickListener { startActivity(BatchCloseActivity.intent(this, batchId)) }
        b.btnReopen.setOnClickListener {
            FarmRepository.batch(batchId)?.let {
                it.status = Batch.STATUS_ACTIVE
                FarmRepository.save()
                render()
            }
        }
        b.btnDelete.setOnClickListener {
            confirm(R.string.confirm_delete_batch) {
                FarmRepository.deleteBatch(batchId)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::b.isInitialized) render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_DATE, expDate)
    }

    /** Two money fields per row, one for each expense category of this enterprise. */
    private fun buildExpenseFields(cfg: BatchConfig) {
        b.fieldsBox.removeAllViews()
        expFields.clear()
        for (pair in cfg.expenses.chunked(2)) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            pair.forEachIndexed { i, key ->
                val f = ViewMoneyFieldBinding.inflate(layoutInflater, row, false)
                f.tvLabel.setText(Ent.expenseLabel(key))
                f.etAmount.isSaveEnabled = false
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                if (i == 0) lp.marginEnd = dp(5) else lp.marginStart = dp(5)
                lp.bottomMargin = dp(14)
                row.addView(f.root, lp)
                expFields[key] = f.etAmount
            }
            if (pair.size == 1) {
                val lp = LinearLayout.LayoutParams(0, 0, 1f)
                lp.marginStart = dp(5)
                row.addView(View(this), lp)
            }
            b.fieldsBox.addView(row)
        }
    }

    private fun render() {
        val batch = FarmRepository.batch(batchId)
        if (batch == null) {
            finish()
            return
        }
        val cfg = Ent.batch(batch.type)
        val closed = batch.isClosed
        val days = Calc.batchDays(batch)
        val unit = getString(cfg.unitRes)

        b.topbar.tvTitle.text = batch.name
        b.tvName.text = batch.name
        var meta = getString(Ent.nameRes(batch.type)) + ", " + getString(R.string.started) + " " +
            Fmt.display(this, batch.startDate) + ", " + Fmt.num(batch.count) + " " + getString(cfg.unitPlRes)
        if (batch.pondArea > 0) meta += ", " + Fmt.num(batch.pondArea) + " " + getString(R.string.acres)
        b.tvMeta.text = meta
        V.styleChip(this, b.tvChip, closed)
        b.tvDays.text = Fmt.num(days) + " " + getString(R.string.days)

        // Slip: expenses by category, then result if sold
        val byCat = Calc.batchByCat(batch)
        val total = Calc.sum(byCat)
        val s = Slip(this, b.slipBox)
        s.head(
            getString(if (closed) R.string.batch_result else R.string.expenses_so_far),
            if (closed) getString(R.string.sold) + " " + Fmt.display(this, batch.saleDate) else getString(R.string.not_sold_yet)
        )
        for (k in listOf("purchase") + cfg.expenses) {
            val v = byCat[k] ?: continue
            val label = if (k == "purchase") getString(cfg.costLabelRes) else getString(Ent.expenseLabel(k))
            s.row(label, Fmt.money(v), sub = true)
        }
        s.total(getString(R.string.total_expenses), Fmt.money(total))
        if (closed) {
            val recv = batch.amountReceived
            val pr = recv - total
            s.row(getString(R.string.amount_received), Fmt.money(recv))
            s.grand(getString(if (pr < 0) R.string.net_loss else R.string.net_profit), pr)
            if (batch.count > 0) {
                s.row(getString(R.string.cost_per_unit, unit), Fmt.money(total / batch.count), sub = true)
                s.row(getString(R.string.profit_per_unit, unit), Fmt.money(pr / batch.count), sub = true, amountColor = profitColor(pr))
            }
        }
        s.foot(getString(R.string.batch_formula))

        b.addGroup.isVisible = !closed
        b.btnClose.isVisible = !closed
        b.btnReopen.isVisible = closed
        if (!closed) {
            if (expDate < batch.startDate) expDate = batch.startDate
            b.tvExpDate.text = Fmt.display(this, expDate)
        }

        // Expense records
        val recs = batch.records.sortedWith(compareByDescending<BatchExpense> { it.date }.thenByDescending { it.id })
        b.tvRecordsTitle.text = getString(R.string.expense_records) + (if (recs.isNotEmpty()) " (" + recs.size + ")" else "")
        b.recordsBox.removeAllViews()
        if (recs.isEmpty()) {
            V.muted(this, b.recordsBox, getString(R.string.no_records_period))
            return
        }
        val list = V.listCard(this, b.recordsBox)
        for (r in recs) {
            val parts = cfg.expenses.mapNotNull { k ->
                val v = r.expenses[k] ?: 0.0
                if (v != 0.0) getString(Ent.expenseLabel(k)) + " " + Fmt.money(v) else null
            }
            var sub = parts.joinToString(", ")
            if (r.note.isNotEmpty()) sub += " (" + r.note + ")"
            val onDelete: (() -> Unit)? = if (closed) null else fun() {
                confirm(R.string.confirm_delete) {
                    batch.records.removeAll { it.id == r.id }
                    FarmRepository.save()
                    render()
                }
            }
            V.recordRow(
                this, list, Fmt.display(this, r.date), sub, Fmt.money(Calc.sum(r.expenses)),
                color(R.color.ink), onDelete = onDelete
            )
        }
    }

    private fun saveExpense() {
        val batch = FarmRepository.batch(batchId) ?: return
        b.tvError.text = ""
        if (Fmt.parse(expDate) == null) {
            b.tvError.setText(R.string.enter_date)
            return
        }
        val exp = LinkedHashMap<String, Double>()
        var any = false
        for ((k, et) in expFields) {
            val v = et.amount()
            exp[k] = v
            if (v > 0) any = true
        }
        if (!any) {
            b.tvError.setText(R.string.enter_something)
            return
        }
        batch.records.add(BatchExpense(FarmRepository.newId(), expDate, exp, b.etExpNote.text.toString().trim()))
        FarmRepository.save()
        toast(R.string.saved)
        expFields.values.forEach { it.setText("") }
        b.etExpNote.setText("")
        render()
    }

    companion object {
        private const val EXTRA_ID = "id"
        private const val STATE_DATE = "expDate"

        fun intent(ctx: Context, id: String): Intent =
            Intent(ctx, BatchDetailActivity::class.java).putExtra(EXTRA_ID, id)
    }
}
