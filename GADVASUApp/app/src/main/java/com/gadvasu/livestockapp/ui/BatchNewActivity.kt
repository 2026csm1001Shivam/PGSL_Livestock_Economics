package com.gadvasu.livestockapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.Batch
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.ActivityBatchNewBinding
import com.gadvasu.livestockapp.util.Fmt
import com.gadvasu.livestockapp.util.amount
import com.gadvasu.livestockapp.util.count
import com.gadvasu.livestockapp.util.pickDate
import com.gadvasu.livestockapp.util.toast

/** Start a batch: name, start date, number of animals, purchase cost (and pond area for fish). */
class BatchNewActivity : BaseActivity() {

    private lateinit var b: ActivityBatchNewBinding
    private lateinit var type: String
    private var startDate = Fmt.today()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireUser() == null) return
        type = intent.getStringExtra(EXTRA_TYPE) ?: Ent.GOATERY
        val cfg = Ent.batch(type)
        b = ActivityBatchNewBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupTopBar(b.topbar, getString(R.string.new_batch))

        b.tvType.setText(Ent.nameRes(type))
        b.tvCountLabel.setText(cfg.countLabelRes)
        b.tvCostLabel.setText(cfg.costLabelRes)
        b.pondGroup.isVisible = cfg.hasPond
        if (savedInstanceState == null) {
            val n = FarmRepository.batches().count { it.type == type } + 1
            b.etName.setText(getString(R.string.batch_word) + " " + n)
        }
        startDate = savedInstanceState?.getString(STATE_DATE) ?: Fmt.today()
        b.tvStartDate.text = Fmt.display(this, startDate)
        b.tvStartDate.setOnClickListener {
            pickDate(startDate) { d ->
                startDate = d
                b.tvStartDate.text = Fmt.display(this, d)
            }
        }
        b.btnStart.setOnClickListener { start() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_DATE, startDate)
    }

    private fun start() {
        b.tvError.text = ""
        if (Fmt.parse(startDate) == null) {
            b.tvError.setText(R.string.enter_date)
            return
        }
        val count = b.etCount.count()
        if (count <= 0) {
            b.tvError.setText(R.string.enter_count)
            return
        }
        val cfg = Ent.batch(type)
        val batch = Batch(
            id = FarmRepository.newId(),
            type = type,
            name = b.etName.text.toString().trim().ifEmpty { getString(R.string.batch_word) },
            startDate = startDate,
            count = count,
            purchaseCost = b.etCost.amount(),
            pondArea = if (cfg.hasPond) b.etPond.amount() else 0.0,
            note = b.etNote.text.toString().trim()
        )
        FarmRepository.addBatch(batch)
        toast(R.string.saved)
        startActivity(BatchDetailActivity.intent(this, batch.id))
        finish()
    }

    companion object {
        private const val EXTRA_TYPE = "type"
        private const val STATE_DATE = "startDate"

        fun intent(ctx: Context, type: String): Intent =
            Intent(ctx, BatchNewActivity::class.java).putExtra(EXTRA_TYPE, type)
    }
}
