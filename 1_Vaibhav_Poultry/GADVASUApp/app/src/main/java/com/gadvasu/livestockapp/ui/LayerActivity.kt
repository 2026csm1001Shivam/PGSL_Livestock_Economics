package com.gadvasu.livestockapp.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.DailyRecord
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.databinding.ActivityLayerBinding
import com.gadvasu.livestockapp.util.Fmt
import com.gadvasu.livestockapp.util.amount
import com.gadvasu.livestockapp.util.count
import com.gadvasu.livestockapp.util.setAmount
import com.gadvasu.livestockapp.util.setCount

/** Layer (egg) daily entry, as in the prototype. */
class LayerActivity : DailyEntryActivity() {

    private lateinit var b: ActivityLayerBinding

    override val type: String = Ent.LAYER
    override val scroller: ScrollView get() = b.scroll
    override val dateField: TextView get() = b.tvDate
    override val noticeView: View get() = b.tvNotice
    override val errorView: TextView get() = b.tvError
    override val saveButton: MaterialButton get() = b.btnSave
    override val resultTitle: TextView get() = b.tvResultTitle
    override val slipBox: LinearLayout get() = b.slipBox
    override val recentBox: LinearLayout get() = b.recentBox
    override val seeAllButton: View get() = b.btnSeeAll

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireUser() == null) return
        b = ActivityLayerBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupTopBar(b.topbar, getString(R.string.layer))
        listOf(b.etEggIncome, b.etOtherIncome, b.etFeed, b.etElectricity, b.etLabour, b.etMedicine, b.etOther)
            .forEach { it.doAfterTextChanged { refreshLive() } }
        initDaily(savedInstanceState)
    }

    override fun currentIncome(): Double = b.etEggIncome.amount() + b.etOtherIncome.amount()

    override fun currentExpense(): Double =
        b.etFeed.amount() + b.etElectricity.amount() + b.etLabour.amount() + b.etMedicine.amount() + b.etOther.amount()

    override fun fillForm(existing: DailyRecord?) {
        val inc = existing?.income
        val exp = existing?.expenses
        b.etEggs.setCount(existing?.production?.toInt() ?: 0)
        b.etEggIncome.setAmount(inc?.get("egg") ?: 0.0)
        b.etOtherIncome.setAmount(inc?.get("other") ?: 0.0)
        b.etFeed.setAmount(exp?.get("feed") ?: 0.0)
        b.etElectricity.setAmount(exp?.get("electricity") ?: 0.0)
        b.etLabour.setAmount(exp?.get("labour") ?: 0.0)
        b.etMedicine.setAmount(exp?.get("medicine") ?: 0.0)
        b.etOther.setAmount(exp?.get("other") ?: 0.0)
        b.etNote.setText(existing?.note ?: "")
    }

    override fun buildRecord(id: String): DailyRecord = DailyRecord(
        id = id,
        date = date,
        production = b.etEggs.count().toDouble(),
        income = linkedMapOf("egg" to b.etEggIncome.amount(), "other" to b.etOtherIncome.amount()),
        expenses = linkedMapOf(
            "feed" to b.etFeed.amount(),
            "electricity" to b.etElectricity.amount(),
            "labour" to b.etLabour.amount(),
            "medicine" to b.etMedicine.amount(),
            "other" to b.etOther.amount()
        ),
        note = b.etNote.text.toString().trim()
    )

    override fun subtitle(r: DailyRecord): String {
        val eggs = if (r.production > 0) Fmt.num(r.production) + " " + getString(R.string.unit_eggs) + ", " else ""
        return eggs + super.subtitle(r)
    }
}
