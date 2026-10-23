package com.gadvasu.livestockapp.ui

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.Batch
import com.gadvasu.livestockapp.data.Calc
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.databinding.ItemBatchBinding
import com.gadvasu.livestockapp.databinding.ItemRecordBinding
import com.gadvasu.livestockapp.databinding.ViewEmptyBinding
import com.gadvasu.livestockapp.databinding.ViewStat3Binding
import com.gadvasu.livestockapp.util.Fmt
import com.gadvasu.livestockapp.util.color
import com.gadvasu.livestockapp.util.dp
import com.gadvasu.livestockapp.util.profitColor

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** Small builders for views that are added from code (lists, cards, headings). */
object V {

    fun text(
        ctx: Context, parent: ViewGroup, value: CharSequence, @StyleRes style: Int,
        top: Int = 0, bottom: Int = 0
    ): TextView {
        val tv = TextView(ctx)
        TextViewCompat.setTextAppearance(tv, style)
        tv.text = value
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = ctx.dp(top)
        lp.bottomMargin = ctx.dp(bottom)
        parent.addView(tv, lp)
        return tv
    }

    fun h1(ctx: Context, parent: ViewGroup, value: CharSequence) = text(ctx, parent, value, R.style.Text_H1, 6, 6)
    fun h2(ctx: Context, parent: ViewGroup, value: CharSequence) = text(ctx, parent, value, R.style.Text_H2, 26, 10)
    fun h3(ctx: Context, parent: ViewGroup, value: CharSequence, top: Int = 18) =
        text(ctx, parent, value, R.style.Text_H3, top, 8)

    fun muted(ctx: Context, parent: ViewGroup, value: CharSequence, top: Int = 0, bottom: Int = 0) =
        text(ctx, parent, value, R.style.Text_Muted, top, bottom)

    /** White rounded card with padding. */
    fun card(ctx: Context, parent: ViewGroup, top: Int = 0): LinearLayout {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        c.setBackgroundResource(R.drawable.bg_card)
        val p = ctx.dp(16)
        c.setPadding(p, p, p, p)
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = ctx.dp(top)
        parent.addView(c, lp)
        return c
    }

    /** Card for a list: rows separated by thin lines. */
    fun listCard(ctx: Context, parent: ViewGroup, top: Int = 0): LinearLayout {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        c.setBackgroundResource(R.drawable.bg_card)
        c.setPadding(ctx.dp(16), ctx.dp(2), ctx.dp(8), ctx.dp(2))
        c.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        c.dividerDrawable = ContextCompat.getDrawable(ctx, R.drawable.divider_h)
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = ctx.dp(top)
        parent.addView(c, lp)
        return c
    }

    /** Card styled like the printed bill ("slip"). */
    fun slipCard(ctx: Context, parent: ViewGroup, top: Int = 0): LinearLayout {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        c.setBackgroundResource(R.drawable.bg_card)
        c.setPadding(ctx.dp(16), ctx.dp(14), ctx.dp(16), ctx.dp(12))
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = ctx.dp(top)
        parent.addView(c, lp)
        return c
    }

    fun empty(
        ctx: Context, parent: ViewGroup, message: CharSequence,
        button: CharSequence? = null, onClick: (() -> Unit)? = null, top: Int = 0
    ) {
        val e = ViewEmptyBinding.inflate(LayoutInflater.from(ctx), parent, false)
        e.tvMessage.text = message
        if (button != null && onClick != null) {
            e.btnAction.isVisible = true
            e.btnAction.text = button
            e.btnAction.setOnClickListener { onClick() }
        }
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = ctx.dp(top)
        parent.addView(e.root, lp)
    }

    /** A dated record row: title, optional tag, details, amount, optional delete. */
    fun recordRow(
        ctx: Context, parent: ViewGroup, title: String, sub: String, amount: String, amountColor: Int,
        tag: String? = null, onClick: (() -> Unit)? = null, onDelete: (() -> Unit)? = null
    ) {
        val rec = ItemRecordBinding.inflate(LayoutInflater.from(ctx), parent, false)
        rec.tvTitle.text = title
        rec.tvSub.text = sub
        rec.tvAmount.text = amount
        rec.tvAmount.setTextColor(amountColor)
        if (tag != null) {
            rec.tvTag.isVisible = true
            rec.tvTag.text = tag
        }
        if (onClick != null) rec.root.setOnClickListener { onClick() }
        if (onDelete != null) {
            rec.btnDelete.isVisible = true
            rec.btnDelete.setOnClickListener { onDelete() }
        }
        parent.addView(rec.root)
    }

    fun styleChip(ctx: Context, chip: TextView, closed: Boolean) {
        chip.setText(if (closed) R.string.completed else R.string.running)
        chip.setBackgroundResource(if (closed) R.drawable.bg_chip_done else R.drawable.bg_chip_run)
        chip.setTextColor(ctx.color(if (closed) R.color.green_ink else R.color.amber_ink))
    }

    /** Batch row used on batch lists, the dashboard and reports. Tapping opens the batch. */
    fun batchRow(ctx: Context, parent: ViewGroup, b: Batch, showType: Boolean) {
        val item = ItemBatchBinding.inflate(LayoutInflater.from(ctx), parent, false)
        val cfg = Ent.batch(b.type)
        val days = Calc.batchDays(b)
        val prefix = if (showType) ctx.getString(Ent.nameRes(b.type)) + ", " else ""
        item.tvName.text = b.name
        styleChip(ctx, item.tvChip, b.isClosed)
        if (b.isClosed) {
            val pr = Calc.batchProfit(b) ?: 0.0
            item.tvSub.text = prefix + ctx.getString(R.string.sold) + " " + Fmt.display(ctx, b.saleDate) +
                ", " + Fmt.num(days) + " " + ctx.getString(R.string.days)
            item.tvAmtLabel.setText(if (pr < 0) R.string.net_loss else R.string.net_profit)
            item.tvAmt.text = Fmt.money(pr)
            item.tvAmt.setTextColor(ctx.profitColor(pr))
        } else {
            item.tvSub.text = prefix + ctx.getString(R.string.started) + " " + Fmt.display(ctx, b.startDate) +
                ", " + Fmt.num(b.count) + " " + ctx.getString(cfg.unitPlRes) +
                ", " + Fmt.num(days) + " " + ctx.getString(R.string.days)
            item.tvAmtLabel.setText(R.string.expenses_so_far)
            item.tvAmt.text = Fmt.money(Calc.batchExpense(b))
            item.tvAmt.setTextColor(ctx.color(R.color.ink))
        }
        item.root.setOnClickListener { ctx.startActivity(BatchDetailActivity.intent(ctx, b.id)) }
        parent.addView(item.root)
    }

    /** Income / Expenses / Profit boxes. */
    fun stat3(ctx: Context, parent: ViewGroup, inc: Double, exp: Double) {
        val s = ViewStat3Binding.inflate(LayoutInflater.from(ctx), parent, false)
        val pr = inc - exp
        s.tvIncKey.setText(R.string.income)
        s.tvInc.text = Fmt.money(inc)
        s.tvExpKey.setText(R.string.expenses)
        s.tvExp.text = Fmt.money(exp)
        s.tvPrKey.setText(if (pr < 0) R.string.loss else R.string.profit)
        s.tvPr.text = Fmt.money(pr)
        s.tvPr.setTextColor(ctx.profitColor(pr))
        parent.addView(s.root)
    }

    fun separator(ctx: Context, parent: ViewGroup) {
        val v = View(ctx)
        v.setBackgroundColor(ctx.color(R.color.line))
        val lp = LinearLayout.LayoutParams(MATCH, ctx.dp(1))
        lp.topMargin = ctx.dp(14)
        lp.bottomMargin = ctx.dp(14)
        parent.addView(v, lp)
    }
}

/**
 * Builds the bill-style summary: label ........ amount, a ruled total line,
 * and a double-ruled grand total (profit / loss).
 */
class Slip(private val ctx: Context, private val root: LinearLayout) {

    init {
        root.removeAllViews()
    }

    fun head(title: String, right: String) {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.BOTTOM
        val t = TextView(ctx)
        TextViewCompat.setTextAppearance(t, R.style.Text_GrandLabel)
        t.text = title
        val r = TextView(ctx)
        TextViewCompat.setTextAppearance(r, R.style.Text_Muted)
        r.text = right
        row.addView(t, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(r, LinearLayout.LayoutParams(WRAP, WRAP))
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.bottomMargin = ctx.dp(8)
        root.addView(row, lp)
        rule(1, R.color.line, top = 0, bottom = 6)
    }

    /** label ........ amount. Returns (label, amount) so callers can update them live. */
    fun row(
        label: String, amount: String, sub: Boolean = false, amountColor: Int? = null
    ): Pair<TextView, TextView> = makeRow(
        label, amount,
        if (sub) R.style.Text_Muted else R.style.Text_Body,
        if (sub) R.style.Text_Amount_Sub else R.style.Text_Amount,
        amountColor
    )

    fun total(label: String, amount: String): Pair<TextView, TextView> {
        rule(2, R.color.ink, top = 6, bottom = 3)
        return makeRow(label, amount, R.style.Text_GrandLabel, R.style.Text_Amount, null)
    }

    fun grand(label: String, value: Double): Pair<TextView, TextView> {
        rule(1, R.color.ink, top = 8, bottom = 2)
        rule(1, R.color.ink, top = 0, bottom = 6)
        val p = makeRow(label, Fmt.money(value), R.style.Text_GrandLabel, R.style.Text_Amount_Grand, ctx.profitColor(value))
        p.first.parent?.let { (it as LinearLayout).gravity = Gravity.CENTER_VERTICAL }
        return p
    }

    fun foot(text: String) {
        val t = TextView(ctx)
        TextViewCompat.setTextAppearance(t, R.style.Text_Tiny)
        t.text = text
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = ctx.dp(10)
        root.addView(t, lp)
    }

    private fun rule(heightDp: Int, colorRes: Int, top: Int, bottom: Int) {
        val v = View(ctx)
        v.setBackgroundColor(ctx.color(colorRes))
        val lp = LinearLayout.LayoutParams(MATCH, ctx.dp(heightDp))
        lp.topMargin = ctx.dp(top)
        lp.bottomMargin = ctx.dp(bottom)
        root.addView(v, lp)
    }

    private fun makeRow(
        label: String, amount: String, @StyleRes labelStyle: Int, @StyleRes amountStyle: Int, amountColor: Int?
    ): Pair<TextView, TextView> {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.BOTTOM
        row.setPadding(0, ctx.dp(5), 0, ctx.dp(5))

        val lbl = TextView(ctx)
        TextViewCompat.setTextAppearance(lbl, labelStyle)
        lbl.text = label
        lbl.maxWidth = ctx.dp(230)

        val lead = View(ctx)
        lead.background = ContextCompat.getDrawable(ctx, R.drawable.dotted_leader)
        lead.setLayerType(View.LAYER_TYPE_SOFTWARE, null) // dashed lines need software drawing

        val amt = TextView(ctx)
        TextViewCompat.setTextAppearance(amt, amountStyle)
        amt.text = amount
        amt.maxLines = 1
        if (amountColor != null) amt.setTextColor(amountColor)

        row.addView(lbl, LinearLayout.LayoutParams(WRAP, WRAP))
        val leadLp = LinearLayout.LayoutParams(0, ctx.dp(4), 1f)
        leadLp.setMargins(ctx.dp(8), 0, ctx.dp(8), ctx.dp(5))
        row.addView(lead, leadLp)
        row.addView(amt, LinearLayout.LayoutParams(WRAP, WRAP))
        root.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
        return lbl to amt
    }
}
