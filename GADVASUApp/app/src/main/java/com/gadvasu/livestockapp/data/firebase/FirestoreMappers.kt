package com.gadvasu.livestockapp.data.firebase

import com.gadvasu.livestockapp.data.Batch
import com.gadvasu.livestockapp.data.BatchExpense
import com.gadvasu.livestockapp.data.Calc
import com.gadvasu.livestockapp.data.DailyRecord
import com.gadvasu.livestockapp.data.FeedItem
import com.gadvasu.livestockapp.data.LivestockDetail
import com.gadvasu.livestockapp.data.Profile
import com.google.firebase.firestore.DocumentSnapshot

/*
 * Converts the app's data classes to Firestore fields and back (schema: see FirestoreSchema.kt).
 * The "fields" maps have no timestamps; CloudSync adds createdAt / updatedAt when it uploads.
 * Firestore returns numbers as Long or Double, so every read goes through num() / int().
 */

private fun Any?.num(): Double = (this as? Number)?.toDouble() ?: 0.0

private fun Any?.int(): Int = (this as? Number)?.toInt() ?: 0

private fun Any?.str(): String = this as? String ?: ""

private fun Any?.asMap(): Map<*, *> = this as? Map<*, *> ?: emptyMap<Any, Any>()

private fun Any?.amounts(): MutableMap<String, Double> {
    val out = LinkedHashMap<String, Double>()
    for ((k, v) in this.asMap()) if (k is String) out[k] = v.num()
    return out
}

// ---------------- users/{uid} ----------------

fun Profile.toUserFields(language: String): Map<String, Any?> {
    val m = linkedMapOf<String, Any?>(
        Schema.UserDoc.PHONE to (if (method == "mobile") value else null),
        Schema.UserDoc.EMAIL to (if (method == "email") value else null),
        Schema.UserDoc.LANGUAGE to language,
        Schema.UserDoc.ROLE to Schema.UserDoc.ROLE_FARMER
    )
    // Only send the name when we have one, so a new phone never blanks it in the cloud
    if (name.isNotBlank()) m[Schema.UserDoc.NAME] = name
    return m
}

/** Profile from users/{uid}; falls back to what the phone knows for missing fields. */
fun DocumentSnapshot.toProfile(uid: String, local: Profile): Profile {
    val phone = getString(Schema.UserDoc.PHONE)
    val email = getString(Schema.UserDoc.EMAIL)
    val method = when {
        !phone.isNullOrEmpty() -> "mobile"
        !email.isNullOrEmpty() -> "email"
        else -> local.method
    }
    val value = phone?.takeIf { it.isNotEmpty() } ?: email?.takeIf { it.isNotEmpty() } ?: local.value
    return Profile(
        key = uid,
        method = method,
        value = value,
        name = getString(Schema.UserDoc.NAME) ?: local.name,
        createdAt = getTimestamp(Schema.UserDoc.CREATED_AT)?.toDate()?.time ?: local.createdAt
    )
}

// ---------------- users/{uid}/dailyRecords/{enterprise}_{date} ----------------

private fun LivestockDetail.toFields(): Map<String, Any> = linkedMapOf(
    Schema.LivestockMap.COWS to cows,
    Schema.LivestockMap.BUFFALOES to buffaloes,
    Schema.LivestockMap.COW_MILK to linkedMapOf(
        Schema.LivestockMap.LITRES to cowLitres,
        Schema.LivestockMap.PRICE_PER_LITRE to cowRate
    ),
    Schema.LivestockMap.BUFFALO_MILK to linkedMapOf(
        Schema.LivestockMap.LITRES to buffaloLitres,
        Schema.LivestockMap.PRICE_PER_LITRE to buffaloRate
    ),
    Schema.LivestockMap.FEEDS to feeds.map {
        linkedMapOf(Schema.LivestockMap.FEED_NAME to it.name, Schema.LivestockMap.FEED_PRICE to it.price)
    }
)

private fun livestockFromMap(m: Map<*, *>): LivestockDetail {
    val cow = m[Schema.LivestockMap.COW_MILK].asMap()
    val buffalo = m[Schema.LivestockMap.BUFFALO_MILK].asMap()
    val feeds = (m[Schema.LivestockMap.FEEDS] as? List<*>).orEmpty().mapNotNull { f ->
        val fm = f as? Map<*, *> ?: return@mapNotNull null
        FeedItem(fm[Schema.LivestockMap.FEED_NAME].str(), fm[Schema.LivestockMap.FEED_PRICE].num())
    }
    return LivestockDetail(
        cows = m[Schema.LivestockMap.COWS].int(),
        buffaloes = m[Schema.LivestockMap.BUFFALOES].int(),
        cowLitres = cow[Schema.LivestockMap.LITRES].num(),
        cowRate = cow[Schema.LivestockMap.PRICE_PER_LITRE].num(),
        buffaloLitres = buffalo[Schema.LivestockMap.LITRES].num(),
        buffaloRate = buffalo[Schema.LivestockMap.PRICE_PER_LITRE].num(),
        feeds = feeds.toMutableList()
    )
}

fun DailyRecord.toDailyFields(enterprise: String): Map<String, Any?> {
    val inc = Calc.income(this)
    val exp = Calc.expense(this)
    return linkedMapOf(
        Schema.DailyDoc.ENTERPRISE to enterprise,
        Schema.DailyDoc.DATE to date,
        Schema.DailyDoc.PRODUCTION to production,
        Schema.DailyDoc.INCOME to LinkedHashMap(income),
        Schema.DailyDoc.EXPENSES to LinkedHashMap(expenses),
        Schema.DailyDoc.TOTAL_INCOME to inc,
        Schema.DailyDoc.TOTAL_EXPENSE to exp,
        Schema.DailyDoc.PROFIT to inc - exp,
        Schema.DailyDoc.NOTE to note,
        Schema.DailyDoc.LIVESTOCK to livestock?.toFields()
    )
}

/** The document ID ("livestock_2026-10-07") becomes the record's id. */
fun DocumentSnapshot.toDailyRecord(): DailyRecord = DailyRecord(
    id = id,
    date = getString(Schema.DailyDoc.DATE) ?: "",
    production = get(Schema.DailyDoc.PRODUCTION).num(),
    income = get(Schema.DailyDoc.INCOME).amounts(),
    expenses = get(Schema.DailyDoc.EXPENSES).amounts(),
    note = getString(Schema.DailyDoc.NOTE) ?: "",
    livestock = (get(Schema.DailyDoc.LIVESTOCK) as? Map<*, *>)?.let { livestockFromMap(it) }
)

// ---------------- users/{uid}/batches/{batchId} ----------------

/** Batch fields without its expense entries (those are a sub-collection). */
fun Batch.toBatchFields(): Map<String, Any?> = linkedMapOf(
    Schema.BatchDoc.ENTERPRISE to type,
    Schema.BatchDoc.NAME to name,
    Schema.BatchDoc.START_DATE to startDate,
    Schema.BatchDoc.COUNT to count,
    Schema.BatchDoc.PURCHASE_COST to purchaseCost,
    Schema.BatchDoc.POND_AREA to pondArea,
    Schema.BatchDoc.NOTE to note,
    Schema.BatchDoc.STATUS to if (isClosed) Schema.BatchDoc.STATUS_COMPLETED else Schema.BatchDoc.STATUS_RUNNING,
    Schema.BatchDoc.SALE_DATE to saleDate.ifEmpty { null },
    Schema.BatchDoc.AMOUNT_RECEIVED to amountReceived,
    Schema.BatchDoc.SALE_NOTE to saleNote,
    Schema.BatchDoc.TOTAL_EXPENSE to Calc.batchExpense(this),
    Schema.BatchDoc.PROFIT to Calc.batchProfit(this)
)

fun DocumentSnapshot.toBatch(expenses: List<BatchExpense>): Batch = Batch(
    id = id,
    type = getString(Schema.BatchDoc.ENTERPRISE) ?: "",
    name = getString(Schema.BatchDoc.NAME) ?: "",
    startDate = getString(Schema.BatchDoc.START_DATE) ?: "",
    count = get(Schema.BatchDoc.COUNT).int(),
    purchaseCost = get(Schema.BatchDoc.PURCHASE_COST).num(),
    pondArea = get(Schema.BatchDoc.POND_AREA).num(),
    note = getString(Schema.BatchDoc.NOTE) ?: "",
    records = expenses.toMutableList(),
    status = if (getString(Schema.BatchDoc.STATUS) == Schema.BatchDoc.STATUS_COMPLETED) Batch.STATUS_CLOSED else Batch.STATUS_ACTIVE,
    saleDate = getString(Schema.BatchDoc.SALE_DATE) ?: "",
    amountReceived = get(Schema.BatchDoc.AMOUNT_RECEIVED).num(),
    saleNote = getString(Schema.BatchDoc.SALE_NOTE) ?: ""
)

// ---------------- users/{uid}/batches/{batchId}/expenses/{expenseId} ----------------

fun BatchExpense.toExpenseFields(): Map<String, Any?> = linkedMapOf(
    Schema.ExpenseDoc.DATE to date,
    Schema.ExpenseDoc.EXPENSES to LinkedHashMap(expenses),
    Schema.ExpenseDoc.TOTAL to Calc.sum(expenses),
    Schema.ExpenseDoc.NOTE to note
)

fun DocumentSnapshot.toBatchExpense(): BatchExpense = BatchExpense(
    id = id,
    date = getString(Schema.ExpenseDoc.DATE) ?: "",
    expenses = get(Schema.ExpenseDoc.EXPENSES).amounts(),
    note = getString(Schema.ExpenseDoc.NOTE) ?: ""
)
