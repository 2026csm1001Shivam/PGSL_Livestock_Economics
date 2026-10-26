package com.gadvasu.livestockapp.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import kotlin.random.Random

/**
 * Single place to read and save data. For now everything is stored on the phone
 * (SharedPreferences, one JSON document per farmer). When the backend is added,
 * only this object needs to change (Firebase Auth + Firestore with offline cache);
 * the screens keep calling the same functions.
 */
object FarmRepository {

    private const val PREFS = "gadvasu_data"
    private const val KEY_LANG = "lang"
    private const val KEY_CURRENT = "current_user"
    private const val USER_PREFIX = "user:"

    private lateinit var prefs: SharedPreferences
    private var current: UserData? = null

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    // ---------- Language ----------

    /** "en", "hi", "pa", or null if the farmer has not chosen yet. */
    var language: String?
        get() = prefs.getString(KEY_LANG, null)
        set(value) {
            prefs.edit().putString(KEY_LANG, value).apply()
        }

    // ---------- Account ----------

    fun user(): UserData? {
        current?.let { return it }
        val key = prefs.getString(KEY_CURRENT, null) ?: return null
        current = load(key)
        return current
    }

    fun login(method: String, value: String, name: String): UserData {
        val key = "$method:$value"
        val u = load(key) ?: UserData(Profile(key, method, value, name, System.currentTimeMillis()))
        if (name.isNotBlank()) u.profile.name = name
        current = u
        prefs.edit().putString(KEY_CURRENT, key).apply()
        save()
        return u
    }

    fun logout() {
        current = null
        prefs.edit().remove(KEY_CURRENT).apply()
    }

    /** Persist the logged-in farmer's data. Call after every change. */
    fun save() {
        val u = current ?: return
        u.updatedAt = System.currentTimeMillis()
        prefs.edit().putString(USER_PREFIX + u.profile.key, u.toJson().toString()).apply()
    }

    private fun load(key: String): UserData? {
        val raw = prefs.getString(USER_PREFIX + key, null) ?: return null
        return try {
            UserData.fromJson(JSONObject(raw))
        } catch (e: Exception) {
            null
        }
    }

    fun newId(): String =
        "r" + java.lang.Long.toString(System.currentTimeMillis(), 36) +
            java.lang.Long.toString(Random.nextLong(0, 60_466_176L), 36)

    // ---------- Daily records ----------

    fun daily(type: String): MutableList<DailyRecord> {
        val u = user() ?: return mutableListOf()
        return u.daily.getOrPut(type) { mutableListOf() }
    }

    fun recordFor(type: String, date: String): DailyRecord? = daily(type).firstOrNull { it.date == date }

    fun latestDaily(type: String): DailyRecord? = daily(type).sortedWith(Calc.byDateDesc).firstOrNull()

    /** Adds the record, or replaces the one already saved for the same date. */
    fun upsertDaily(type: String, record: DailyRecord) {
        val list = daily(type)
        val i = list.indexOfFirst { it.date == record.date }
        if (i >= 0) list[i] = record else list.add(record)
        save()
    }

    fun deleteDaily(type: String, id: String) {
        daily(type).removeAll { it.id == id }
        save()
    }

    // ---------- Batches ----------

    fun batches(): MutableList<Batch> = user()?.batches ?: mutableListOf()

    fun batch(id: String): Batch? = batches().firstOrNull { it.id == id }

    fun addBatch(b: Batch) {
        batches().add(b)
        save()
    }

    fun deleteBatch(id: String) {
        batches().removeAll { it.id == id }
        save()
    }

    fun clearAll() {
        val u = user() ?: return
        u.daily.clear()
        u.batches.clear()
        save()
    }
}
