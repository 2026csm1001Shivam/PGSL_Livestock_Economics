package com.gadvasu.livestockapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.ActivityBatchListBinding

/** All batches of one enterprise: running first, then completed. */
class BatchListActivity : BaseActivity() {

    private lateinit var b: ActivityBatchListBinding
    private lateinit var type: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireUser() == null) return
        type = intent.getStringExtra(EXTRA_TYPE) ?: Ent.GOATERY
        val cfg = Ent.batch(type)
        b = ActivityBatchListBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupTopBar(b.topbar, getString(Ent.nameRes(type)))
        b.tvTitle.setText(Ent.nameRes(type))
        b.tvHelp.setText(cfg.helpRes)
        b.btnStart.setOnClickListener { startActivity(BatchNewActivity.intent(this, type)) }
    }

    override fun onResume() {
        super.onResume()
        if (::b.isInitialized) render()
    }

    private fun render() {
        val box = b.listBox
        box.removeAllViews()
        val all = FarmRepository.batches().filter { it.type == type }
        val active = all.filter { !it.isClosed }.sortedByDescending { it.startDate }
        val done = all.filter { it.isClosed }.sortedByDescending { it.saleDate }
        if (all.isEmpty()) V.empty(this, box, getString(R.string.no_batches), top = 20)
        if (active.isNotEmpty()) {
            V.h2(this, box, getString(R.string.active_batches))
            val list = V.listCard(this, box)
            active.forEach { V.batchRow(this, list, it, false) }
        }
        if (done.isNotEmpty()) {
            V.h2(this, box, getString(R.string.completed_batches))
            val list = V.listCard(this, box)
            done.forEach { V.batchRow(this, list, it, false) }
        }
    }

    companion object {
        private const val EXTRA_TYPE = "type"

        fun intent(ctx: Context, type: String): Intent =
            Intent(ctx, BatchListActivity::class.java).putExtra(EXTRA_TYPE, type)
    }
}
