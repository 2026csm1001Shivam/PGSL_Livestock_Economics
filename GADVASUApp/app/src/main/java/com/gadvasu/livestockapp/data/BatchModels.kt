package com.gadvasu.livestockapp.data

import org.json.JSONArray
import org.json.JSONObject

/* Batch enterprises (Broiler, Goatery, Fishery): a batch and its expense entries. */

/** An expense entry inside a batch. */
data class BatchExpense(
    val id: String,
    val date: String,
    val expenses: Map<String, Double>,
    val note: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("date", date)
        .put("expenses", mapToJson(expenses))
        .put("note", note)

    companion object {
        fun fromJson(o: JSONObject) = BatchExpense(
            id = o.optString("id", ""),
            date = o.optString("date", ""),
            expenses = jsonToMap(o.optJSONObject("expenses")),
            note = o.optString("note", "")
        )
    }
}

/** A batch for Broiler, Goatery or Fishery. Profit is known only after it is sold. */
data class Batch(
    val id: String,
    val type: String,
    var name: String,
    var startDate: String,
    var count: Int,
    var purchaseCost: Double = 0.0,
    var pondArea: Double = 0.0,
    var note: String = "",
    val records: MutableList<BatchExpense> = mutableListOf(),
    var status: String = STATUS_ACTIVE,
    var saleDate: String = "",
    var amountReceived: Double = 0.0,
    var saleNote: String = ""
) {
    val isClosed: Boolean get() = status == STATUS_CLOSED

    fun toJson(): JSONObject {
        val arr = JSONArray()
        records.forEach { arr.put(it.toJson()) }
        return JSONObject()
            .put("id", id)
            .put("type", type)
            .put("name", name)
            .put("startDate", startDate)
            .put("count", count)
            .put("purchaseCost", purchaseCost.finiteOrZero())
            .put("pondArea", pondArea.finiteOrZero())
            .put("note", note)
            .put("records", arr)
            .put("status", status)
            .put("saleDate", saleDate)
            .put("amountReceived", amountReceived.finiteOrZero())
            .put("saleNote", saleNote)
    }

    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_CLOSED = "closed"

        fun fromJson(o: JSONObject): Batch {
            val records = mutableListOf<BatchExpense>()
            val arr = o.optJSONArray("records")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val r = arr.optJSONObject(i)
                    if (r != null) records.add(BatchExpense.fromJson(r))
                }
            }
            return Batch(
                id = o.optString("id", ""),
                type = o.optString("type", ""),
                name = o.optString("name", ""),
                startDate = o.optString("startDate", ""),
                count = o.optInt("count", 0),
                purchaseCost = o.optDouble("purchaseCost", 0.0).finiteOrZero(),
                pondArea = o.optDouble("pondArea", 0.0).finiteOrZero(),
                note = o.optString("note", ""),
                records = records,
                status = o.optString("status", STATUS_ACTIVE),
                saleDate = o.optString("saleDate", ""),
                amountReceived = o.optDouble("amountReceived", 0.0).finiteOrZero(),
                saleNote = o.optString("saleNote", "")
            )
        }
    }
}
