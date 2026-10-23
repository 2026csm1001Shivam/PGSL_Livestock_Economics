package com.gadvasu.livestockapp.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gadvasu.livestockapp.R

/** Settings for a batch enterprise (Broiler, Goatery, Fishery). */
data class BatchConfig(
    val type: String,
    @StringRes val helpRes: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val unitRes: Int,
    @StringRes val unitPlRes: Int,
    @StringRes val countLabelRes: Int,
    @StringRes val costLabelRes: Int,
    val hasPond: Boolean,
    val expenses: List<String>
)

/** The five enterprises and their category lists. */
object Ent {
    const val LAYER = "layer"
    const val LIVESTOCK = "livestock"
    const val BROILER = "broiler"
    const val GOATERY = "goatery"
    const val FISHERY = "fishery"

    val ORDER = listOf(LAYER, LIVESTOCK, BROILER, GOATERY, FISHERY)
    val DAILY = listOf(LAYER, LIVESTOCK)

    fun isDaily(type: String): Boolean = type in DAILY

    @StringRes
    fun nameRes(type: String): Int = when (type) {
        LAYER -> R.string.layer
        LIVESTOCK -> R.string.livestock
        BROILER -> R.string.broiler
        GOATERY -> R.string.goatery
        else -> R.string.fishery
    }

    // Daily enterprises: category keys used in DailyRecord.income / .expenses
    val LAYER_INCOME = listOf("egg", "other")
    val LAYER_EXPENSES = listOf("feed", "electricity", "labour", "medicine", "other")
    val LIVESTOCK_INCOME = listOf("cowMilk", "buffaloMilk", "byproduct", "other")
    val LIVESTOCK_EXPENSES = listOf("feed", "electricity", "labour", "vet", "water", "other")

    private val BATCH = mapOf(
        BROILER to BatchConfig(
            BROILER, R.string.broiler_help, R.drawable.ic_chick, R.string.bird, R.string.birds_unit,
            R.string.birds_count, R.string.chick_cost, false,
            listOf("feed", "electricity", "labour", "medicine", "other")
        ),
        GOATERY to BatchConfig(
            GOATERY, R.string.goatery_help, R.drawable.ic_goat, R.string.goat, R.string.goats_unit,
            R.string.goats_count, R.string.goat_cost, false,
            listOf("feed", "labour", "vet", "electricity", "water", "other")
        ),
        FISHERY to BatchConfig(
            FISHERY, R.string.fishery_help, R.drawable.ic_fish, R.string.fish, R.string.fish_unit,
            R.string.seed_count, R.string.seed_cost, true,
            listOf("feed", "labour", "electricity", "water", "medicine", "other")
        )
    )

    fun batch(type: String): BatchConfig = BATCH[type] ?: BATCH.getValue(GOATERY)

    @StringRes
    fun incomeLabel(key: String): Int = when (key) {
        "egg" -> R.string.egg_income
        "cowMilk" -> R.string.cow_milk_sales
        "buffaloMilk" -> R.string.buffalo_milk_sales
        "byproduct" -> R.string.byproduct_income
        else -> R.string.other_income
    }

    @StringRes
    fun expenseLabel(key: String): Int = when (key) {
        "feed" -> R.string.feed
        "electricity" -> R.string.electricity
        "labour" -> R.string.labour
        "medicine" -> R.string.medicine
        "vet" -> R.string.vet
        "water" -> R.string.water
        "purchase" -> R.string.purchase_cost
        else -> R.string.other
    }
}
