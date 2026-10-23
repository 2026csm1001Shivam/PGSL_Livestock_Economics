package com.gadvasu.livestockapp.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.DailyRecord
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.data.FeedItem
import com.gadvasu.livestockapp.data.LivestockDetail
import com.gadvasu.livestockapp.databinding.ActivityLivestockBinding
import com.gadvasu.livestockapp.databinding.ItemFeedRowBinding
import com.gadvasu.livestockapp.util.Fmt
import com.gadvasu.livestockapp.util.amount
import com.gadvasu.livestockapp.util.count
import com.gadvasu.livestockapp.util.setAmount
import com.gadvasu.livestockapp.util.setCount

/**
 * Livestock (dairy) daily entry:
 *  - Your animals: number of cows and buffaloes
 *  - Milk sold today: Cow milk and Buffalo milk, each with litres and today's price per litre
 *  - By-products and other income
 *  - Feeds: each feed's name and price, "Add another feed" for more
 *  - Other daily expenses
 * Profit = (cow milk + buffalo milk + by-products + other) - (all feeds + other expenses)
 */
class LivestockActivity : DailyEntryActivity() {

    private lateinit var b: ActivityLivestockBinding
    private val feedRows = mutableListOf<ItemFeedRowBinding>()

    override val type: String = Ent.LIVESTOCK
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
        b = ActivityLivestockBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupTopBar(b.topbar, getString(R.string.livestock))

        listOf(
            b.etCowLitres, b.etCowRate, b.etBuffaloLitres, b.etBuffaloRate,
            b.etByproduct, b.etOtherIncome,
            b.etLabour, b.etElectricity, b.etWater, b.etVet, b.etOther
        ).forEach { it.doAfterTextChanged { onFormChanged() } }

        b.btnAddFeed.setOnClickListener { addFeedRow(FeedItem(), focus = true) }
        initDaily(savedInstanceState)
    }

    // ---------- Feed rows ----------

    private fun addFeedRow(item: FeedItem, focus: Boolean = false) {
        val row = ItemFeedRowBinding.inflate(layoutInflater, b.feedContainer, false)
        // Several rows share the same view IDs, so we manage their text ourselves
        row.etFeedName.isSaveEnabled = false
        row.etFeedPrice.isSaveEnabled = false
        row.etFeedName.setText(item.name)
        row.etFeedPrice.setAmount(item.price)
        row.etFeedName.doAfterTextChanged { b.tvError.text = "" }
        row.etFeedPrice.doAfterTextChanged { onFormChanged() }
        row.btnRemoveFeed.setOnClickListener { removeFeedRow(row) }
        b.feedContainer.addView(row.root)
        feedRows.add(row)
        if (focus) {
            row.etFeedName.requestFocus()
            WindowCompat.getInsetsController(window, row.etFeedName).show(WindowInsetsCompat.Type.ime())
        }
        onFormChanged()
    }

    private fun removeFeedRow(row: ItemFeedRowBinding) {
        if (feedRows.size <= 1) {
            // Keep one row on screen so the farmer is always asked for a feed
            row.etFeedName.setText("")
            row.etFeedPrice.setText("")
            return
        }
        b.feedContainer.removeView(row.root)
        feedRows.remove(row)
        onFormChanged()
    }

    private fun feedTotal(): Double = feedRows.sumOf { it.etFeedPrice.amount() }

    // ---------- Live totals ----------

    private fun calcText(litres: Double, rate: Double): String =
        if (litres == 0.0 && rate == 0.0) ""
        else Fmt.num(litres) + " " + getString(R.string.unit_litres) + " × " + Fmt.money(rate)

    private fun cowIncome(): Double = b.etCowLitres.amount() * b.etCowRate.amount()
    private fun buffaloIncome(): Double = b.etBuffaloLitres.amount() * b.etBuffaloRate.amount()

    private fun onFormChanged() {
        if (!::b.isInitialized) return
        val cow = cowIncome()
        val buffalo = buffaloIncome()
        b.tvCowCalc.text = calcText(b.etCowLitres.amount(), b.etCowRate.amount())
        b.tvCowTotal.text = Fmt.money(cow)
        b.tvBuffaloCalc.text = calcText(b.etBuffaloLitres.amount(), b.etBuffaloRate.amount())
        b.tvBuffaloTotal.text = Fmt.money(buffalo)
        b.tvMilkTotal.text = Fmt.money(cow + buffalo)
        b.tvFeedTotal.text = Fmt.money(feedTotal())
        refreshLive()
    }

    override fun currentIncome(): Double =
        cowIncome() + buffaloIncome() + b.etByproduct.amount() + b.etOtherIncome.amount()

    override fun currentExpense(): Double =
        feedTotal() + b.etLabour.amount() + b.etElectricity.amount() + b.etWater.amount() +
            b.etVet.amount() + b.etOther.amount()

    // ---------- Load / save ----------

    override fun fillForm(existing: DailyRecord?) {
        val d = existing?.livestock
        // For a new day, carry over animal numbers and milk prices from the last record
        val last = if (existing == null) FarmRepository.latestDaily(type)?.livestock else null
        val herd = d ?: last
        b.etCows.setCount(herd?.cows ?: 0)
        b.etBuffaloes.setCount(herd?.buffaloes ?: 0)
        b.etCowLitres.setAmount(d?.cowLitres ?: 0.0)
        b.etCowRate.setAmount(d?.cowRate ?: last?.cowRate ?: 0.0)
        b.etBuffaloLitres.setAmount(d?.buffaloLitres ?: 0.0)
        b.etBuffaloRate.setAmount(d?.buffaloRate ?: last?.buffaloRate ?: 0.0)
        b.tvPrefilled.isVisible = existing == null && last != null

        val inc = existing?.income
        val exp = existing?.expenses
        b.etByproduct.setAmount(inc?.get("byproduct") ?: 0.0)
        b.etOtherIncome.setAmount(inc?.get("other") ?: 0.0)
        b.etLabour.setAmount(exp?.get("labour") ?: 0.0)
        b.etElectricity.setAmount(exp?.get("electricity") ?: 0.0)
        b.etWater.setAmount(exp?.get("water") ?: 0.0)
        b.etVet.setAmount(exp?.get("vet") ?: 0.0)
        b.etOther.setAmount(exp?.get("other") ?: 0.0)
        b.etNote.setText(existing?.note ?: "")

        b.feedContainer.removeAllViews()
        feedRows.clear()
        val feeds = d?.feeds.orEmpty()
        if (feeds.isEmpty()) {
            val oldFeed = exp?.get("feed") ?: 0.0
            addFeedRow(if (oldFeed > 0) FeedItem(getString(R.string.feed), oldFeed) else FeedItem())
        } else {
            feeds.forEach { addFeedRow(it.copy()) }
        }
        onFormChanged()
    }

    override fun buildRecord(id: String): DailyRecord? {
        val feeds = mutableListOf<FeedItem>()
        for (row in feedRows) {
            val name = row.etFeedName.text.toString().trim()
            val price = row.etFeedPrice.amount()
            if (name.isEmpty() && price == 0.0) continue
            if (name.isEmpty()) {
                b.tvError.setText(R.string.enter_feed_name)
                row.etFeedName.requestFocus()
                return null
            }
            feeds.add(FeedItem(name, price))
        }
        val detail = LivestockDetail(
            cows = b.etCows.count(),
            buffaloes = b.etBuffaloes.count(),
            cowLitres = b.etCowLitres.amount(),
            cowRate = b.etCowRate.amount(),
            buffaloLitres = b.etBuffaloLitres.amount(),
            buffaloRate = b.etBuffaloRate.amount(),
            feeds = feeds
        )
        val income = linkedMapOf(
            "cowMilk" to detail.cowIncome,
            "buffaloMilk" to detail.buffaloIncome,
            "byproduct" to b.etByproduct.amount(),
            "other" to b.etOtherIncome.amount()
        )
        val expenses = linkedMapOf(
            "feed" to detail.feedTotal,
            "electricity" to b.etElectricity.amount(),
            "labour" to b.etLabour.amount(),
            "vet" to b.etVet.amount(),
            "water" to b.etWater.amount(),
            "other" to b.etOther.amount()
        )
        return DailyRecord(
            id = id,
            date = date,
            production = detail.cowLitres + detail.buffaloLitres,
            income = income,
            expenses = expenses,
            note = b.etNote.text.toString().trim(),
            livestock = detail
        )
    }

    override fun subtitle(r: DailyRecord): String {
        val litres = if (r.production > 0) Fmt.num(r.production) + " " + getString(R.string.unit_litres) + ", " else ""
        return litres + super.subtitle(r)
    }
}
