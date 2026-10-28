package com.gadvasu.livestockapp.data.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/*
 * Firestore schema for the GADVASU Livestock Economics app (see SCHEMA.md).
 *
 * users/{uid}
 *  ├── dailyRecords/{enterprise}_{date}      Livestock and Layer, one per enterprise per day
 *  └── batches/{batchId}                     Broiler, Goatery, Fishery
 *        └── expenses/{expenseId}
 *
 * All collection and field names live here, so a typo can't create a second spelling.
 */
object Schema {

    const val USERS = "users"
    const val DAILY_RECORDS = "dailyRecords"
    const val BATCHES = "batches"
    const val EXPENSES = "expenses"

    /** users/{uid} */
    object UserDoc {
        const val NAME = "name"
        const val PHONE = "phone"
        const val EMAIL = "email"
        const val LANGUAGE = "language"
        const val ROLE = "role"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"

        const val ROLE_FARMER = "farmer"
    }

    /** users/{uid}/dailyRecords/{enterprise}_{date} */
    object DailyDoc {
        const val ENTERPRISE = "enterprise"
        const val DATE = "date"
        const val PRODUCTION = "production"
        const val INCOME = "income"
        const val EXPENSES = "expenses"
        const val TOTAL_INCOME = "totalIncome"
        const val TOTAL_EXPENSE = "totalExpense"
        const val PROFIT = "profit"
        const val NOTE = "note"
        const val LIVESTOCK = "livestock"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"

        /** One record per enterprise per day, e.g. "livestock_2026-10-07". */
        fun id(enterprise: String, date: String): String = "${enterprise}_$date"
    }

    /** The "livestock" map inside a Livestock daily record. */
    object LivestockMap {
        const val COWS = "cows"
        const val BUFFALOES = "buffaloes"
        const val COW_MILK = "cowMilk"
        const val BUFFALO_MILK = "buffaloMilk"
        const val LITRES = "litres"
        const val PRICE_PER_LITRE = "pricePerLitre"
        const val FEEDS = "feeds"
        const val FEED_NAME = "name"
        const val FEED_PRICE = "price"
    }

    /** users/{uid}/batches/{batchId} */
    object BatchDoc {
        const val ENTERPRISE = "enterprise"
        const val NAME = "name"
        const val START_DATE = "startDate"
        const val COUNT = "count"
        const val PURCHASE_COST = "purchaseCost"
        const val POND_AREA = "pondArea"
        const val NOTE = "note"
        const val STATUS = "status"
        const val SALE_DATE = "saleDate"
        const val AMOUNT_RECEIVED = "amountReceived"
        const val SALE_NOTE = "saleNote"
        const val TOTAL_EXPENSE = "totalExpense"
        const val PROFIT = "profit"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"

        const val STATUS_RUNNING = "running"
        const val STATUS_COMPLETED = "completed"
    }

    /** users/{uid}/batches/{batchId}/expenses/{expenseId} */
    object ExpenseDoc {
        const val DATE = "date"
        const val EXPENSES = "expenses"
        const val TOTAL = "total"
        const val NOTE = "note"
        const val CREATED_AT = "createdAt"
    }
}

/** Builds document and collection references for one farmer. */
class FarmPaths(private val db: FirebaseFirestore, private val uid: String) {

    fun user(): DocumentReference = db.collection(Schema.USERS).document(uid)

    fun dailyRecords(): CollectionReference = user().collection(Schema.DAILY_RECORDS)

    fun dailyRecord(enterprise: String, date: String): DocumentReference =
        dailyRecords().document(Schema.DailyDoc.id(enterprise, date))

    fun batches(): CollectionReference = user().collection(Schema.BATCHES)

    fun batch(batchId: String): DocumentReference = batches().document(batchId)

    fun expenses(batchId: String): CollectionReference = batch(batchId).collection(Schema.EXPENSES)

    fun expense(batchId: String, expenseId: String): DocumentReference = expenses(batchId).document(expenseId)
}
