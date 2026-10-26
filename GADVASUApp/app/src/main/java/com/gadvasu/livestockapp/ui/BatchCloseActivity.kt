package com.gadvasu.livestockapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.Batch
import com.gadvasu.livestockapp.data.Calc
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.ActivityBatchCloseBinding
import com.gadvasu.livestockapp.util.Fmt
import com.gadvasu.livestockapp.util.amount
import com.gadvasu.livestockapp.util.pickDate
import com.gadvasu.livestockapp.util.plainNumber
import com.gadvasu.livestockapp.util.profitColor
import com.gadvasu.livestockapp.util.toast

/** Close a batch after sale: sale date + amount received; live net profit. */
class BatchCloseActivity : BaseActivity() {

    private lateinit var b: ActivityBatchCloseBinding
    private lateinit var batchId: String
    private var saleDate = Fmt.today()
    private var liveRecv: TextView? = null
    private var liveLbl: TextView? = null
    private var livePr: TextView? = null

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
        b = ActivityBatchCloseBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupTopBar(b.topbar, getString(R.string.close_batch))
        b.tvSub.text = batch.name + ", " + getString(Ent.nameRes(batch.type))

        saleDate = savedInstanceState?.getString(STATE_DATE) ?: batch.saleDate.ifEmpty { Fmt.today() }
        if (saleDate < batch.startDate) saleDate = batch.startDate
        b.tvSaleDate.text = Fmt.display(this, saleDate)
        b.tvSaleDate.setOnClickListener {
            pickDate(saleDate, batch.startDate) { d ->
                saleDate = d
                b.tvSaleDate.text = Fmt.display(this, d)
            }
        }
        if (savedInstanceState == null) {
            if (batch.amountReceived > 0) b.etAmount.setText(plainNumber(batch.amountReceived))
            b.etNote.setText(batch.saleNote)
        }
        buildSlip(batch)
        b.etAmount.doAfterTextChanged { refresh() }
        b.btnSave.setOnClickListener { save() }
        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_DATE, saleDate)
    }

    private fun buildSlip(batch: Batch) {
        val s = Slip(this, b.slipBox)
        liveRecv = s.row(getString(R.string.amount_received), Fmt.money(0.0)).second
        s.row(getString(R.string.total_expenses), Fmt.money(Calc.batchExpense(batch)))
        val g = s.grand(getString(R.string.net_profit), 0.0)
        liveLbl = g.first
        livePr = g.second
        s.foot(getString(R.string.batch_formula))
    }

    private fun refresh() {
        val batch = FarmRepository.batch(batchId) ?: return
        val recv = b.etAmount.amount()
        val pr = recv - Calc.batchExpense(batch)
        liveRecv?.text = Fmt.money(recv)
        livePr?.text = Fmt.money(pr)
        livePr?.setTextColor(profitColor(pr))
        liveLbl?.setText(if (pr < 0) R.string.net_loss else R.string.net_profit)
    }

    private fun save() {
        val batch = FarmRepository.batch(batchId) ?: return
        b.tvError.text = ""
        if (Fmt.parse(saleDate) == null || saleDate < batch.startDate) {
            b.tvError.setText(R.string.enter_date)
            return
        }
        val raw = b.etAmount.text.toString().trim().toDoubleOrNull()
        if (raw == null || !raw.isFinite() || raw < 0) {
            b.tvError.setText(R.string.enter_amount)
            return
        }
        batch.status = Batch.STATUS_CLOSED
        batch.saleDate = saleDate
        batch.amountReceived = raw
        batch.saleNote = b.etNote.text.toString().trim()
        FarmRepository.save()
        toast(R.string.saved)
        finish()
    }

    companion object {
        private const val EXTRA_ID = "id"
        private const val STATE_DATE = "saleDate"

        fun intent(ctx: Context, id: String): Intent =
            Intent(ctx, BatchCloseActivity::class.java).putExtra(EXTRA_ID, id)
    }
}
