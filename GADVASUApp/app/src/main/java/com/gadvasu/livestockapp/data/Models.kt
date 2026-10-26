package com.gadvasu.livestockapp.data

import org.json.JSONArray
import org.json.JSONObject

/*
 * Plain data classes for everything the farmer records.
 * Each has toJson()/fromJson() so the local store can save it. When the
 * Firebase backend is added, the same shapes map onto Firestore documents
 * (users/{uid}, dailyRecords/{id}, batches/{batchId}, expenses/{expenseId}).
 */

internal fun Double.finiteOrZero(): Double = if (this.isFinite()) this else 0.0

internal fun mapToJson(m: Map<String, Double>): JSONObject {
    val o = JSONObject()
    for ((k, v) in m) o.put(k, v.finiteOrZero())
    return o
}

internal fun jsonToMap(o: JSONObject?): MutableMap<String, Double> {
    val m = LinkedHashMap<String, Double>()
    if (o == null) return m
    val keys = o.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        m[k] = o.optDouble(k, 0.0).finiteOrZero()
    }
    return m
}

/**
 * One day's record for a daily enterprise (Livestock or Layer).
 * income / expenses hold amounts by category key; profit = sum(income) - sum(expenses).
 * Only one record per enterprise per date.
 */
data class DailyRecord(
    val id: String,
    var date: String,
    var production: Double = 0.0,
    var income: MutableMap<String, Double> = LinkedHashMap(),
    var expenses: MutableMap<String, Double> = LinkedHashMap(),
    var note: String = "",
    var livestock: LivestockDetail? = null
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
            .put("id", id)
            .put("date", date)
            .put("production", production.finiteOrZero())
            .put("income", mapToJson(income))
            .put("expenses", mapToJson(expenses))
            .put("note", note)
        livestock?.let { o.put("livestock", it.toJson()) }
        return o
    }

    companion object {
        fun fromJson(o: JSONObject) = DailyRecord(
            id = o.optString("id", ""),
            date = o.optString("date", ""),
            production = o.optDouble("production", 0.0).finiteOrZero(),
            income = jsonToMap(o.optJSONObject("income")),
            expenses = jsonToMap(o.optJSONObject("expenses")),
            note = o.optString("note", ""),
            livestock = o.optJSONObject("livestock")?.let { LivestockDetail.fromJson(it) }
        )
    }
}

/** Who is logged in: method is "mobile" or "email"; key = "method:value". */
data class Profile(
    val key: String,
    val method: String,
    val value: String,
    var name: String,
    val createdAt: Long
) {
    fun toJson(): JSONObject = JSONObject()
        .put("key", key)
        .put("method", method)
        .put("value", value)
        .put("name", name)
        .put("createdAt", createdAt)

    companion object {
        fun fromJson(o: JSONObject) = Profile(
            key = o.optString("key", ""),
            method = o.optString("method", "mobile"),
            value = o.optString("value", ""),
            name = o.optString("name", ""),
            createdAt = o.optLong("createdAt", 0L)
        )
    }
}

/** Everything stored for one farmer. */
class UserData(
    val profile: Profile,
    val daily: MutableMap<String, MutableList<DailyRecord>> = LinkedHashMap(),
    val batches: MutableList<Batch> = mutableListOf(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val d = JSONObject()
        for ((type, list) in daily) {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            d.put(type, arr)
        }
        val b = JSONArray()
        batches.forEach { b.put(it.toJson()) }
        return JSONObject()
            .put("profile", profile.toJson())
            .put("daily", d)
            .put("batches", b)
            .put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(o: JSONObject): UserData {
            val profile = Profile.fromJson(o.optJSONObject("profile") ?: JSONObject())
            val daily = LinkedHashMap<String, MutableList<DailyRecord>>()
            val d = o.optJSONObject("daily")
            if (d != null) {
                val keys = d.keys()
                while (keys.hasNext()) {
                    val type = keys.next()
                    val list = mutableListOf<DailyRecord>()
                    val arr = d.optJSONArray(type)
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val r = arr.optJSONObject(i)
                            if (r != null) list.add(DailyRecord.fromJson(r))
                        }
                    }
                    daily[type] = list
                }
            }
            val batches = mutableListOf<Batch>()
            val b = o.optJSONArray("batches")
            if (b != null) {
                for (i in 0 until b.length()) {
                    val x = b.optJSONObject(i)
                    if (x != null) batches.add(Batch.fromJson(x))
                }
            }
            return UserData(profile, daily, batches, o.optLong("updatedAt", System.currentTimeMillis()))
        }
    }
}
