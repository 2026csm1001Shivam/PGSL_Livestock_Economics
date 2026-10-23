package com.gadvasu.livestockapp.data

import org.json.JSONArray
import org.json.JSONObject

/* Livestock-only data: herd size, milk by type, and the list of feeds. */

/** One feed used on a day, e.g. "Silage" ₹320. */
data class FeedItem(
    var name: String = "",
    var price: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("price", price.finiteOrZero())

    companion object {
        fun fromJson(o: JSONObject) = FeedItem(
            name = o.optString("name", ""),
            price = o.optDouble("price", 0.0).finiteOrZero()
        )
    }
}

/** Livestock-only details of a daily record: herd size, milk by type, feed list. */
data class LivestockDetail(
    var cows: Int = 0,
    var buffaloes: Int = 0,
    var cowLitres: Double = 0.0,
    var cowRate: Double = 0.0,
    var buffaloLitres: Double = 0.0,
    var buffaloRate: Double = 0.0,
    var feeds: MutableList<FeedItem> = mutableListOf()
) {
    val cowIncome: Double get() = cowLitres * cowRate
    val buffaloIncome: Double get() = buffaloLitres * buffaloRate
    val feedTotal: Double get() = feeds.sumOf { it.price }

    fun toJson(): JSONObject {
        val arr = JSONArray()
        feeds.forEach { arr.put(it.toJson()) }
        return JSONObject()
            .put("cows", cows)
            .put("buffaloes", buffaloes)
            .put("cowLitres", cowLitres.finiteOrZero())
            .put("cowRate", cowRate.finiteOrZero())
            .put("buffaloLitres", buffaloLitres.finiteOrZero())
            .put("buffaloRate", buffaloRate.finiteOrZero())
            .put("feeds", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): LivestockDetail {
            val feeds = mutableListOf<FeedItem>()
            val arr = o.optJSONArray("feeds")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val f = arr.optJSONObject(i)
                    if (f != null) feeds.add(FeedItem.fromJson(f))
                }
            }
            return LivestockDetail(
                cows = o.optInt("cows", 0),
                buffaloes = o.optInt("buffaloes", 0),
                cowLitres = o.optDouble("cowLitres", 0.0).finiteOrZero(),
                cowRate = o.optDouble("cowRate", 0.0).finiteOrZero(),
                buffaloLitres = o.optDouble("buffaloLitres", 0.0).finiteOrZero(),
                buffaloRate = o.optDouble("buffaloRate", 0.0).finiteOrZero(),
                feeds = feeds
            )
        }
    }
}
