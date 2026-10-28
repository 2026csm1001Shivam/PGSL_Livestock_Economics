package com.gadvasu.livestockapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.gadvasu.livestockapp.data.firebase.CloudSync
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random

/**
 * Single place to read and save data.
 *
 * - The logged-in farmer is the Firebase user (FirebaseAuth). Their data is keyed by the Firebase uid.
 * - Every change is saved on the phone first (works offline, opens instantly), then
 *   CloudSync uploads it to Cloud Firestore.
 * - At login / app start, syncFromCloud() downloads the farmer's records, so the same
 *   farmer sees the same data on any phone.
 *
 * Screens only call the functions below; they don't talk to Firebase directly.
 */
object FarmRepository {

    private const val TAG = "FarmRepository"
    private const val PREFS = "gadvasu_data"
    private const val KEY_LANG = "lang"
    private const val USER_PREFIX = "user:"

    private lateinit var prefs: SharedPreferences
    private var current: UserData? = null

    /** Goes up on every local change; used to avoid overwriting a change with an older download. */
    private var version = 0L

    private val scope = MainScope()
    private val listeners = LinkedHashSet<() -> Unit>()

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
        CloudSync.init(context)
    }

    // ---------- Language ----------

    /** "en", "hi", "pa", or null if the farmer has not chosen yet. Saved to the cloud profile too. */
    var language: String?
        get() = prefs.getString(KEY_LANG, null)
        set(value) {
            prefs.edit().putString(KEY_LANG, value).apply()
            if (user() != null) save()
        }

    private fun lang(): String = language ?: "en"

    // ---------- Account ----------

    /** The logged-in farmer, or null if nobody is signed in to Firebase. */
    fun user(): UserData? {
        val uid = CloudSync.uid
        if (uid == null) {
            current = null
            return null
        }
        current?.let { if (it.profile.key == uid) return it }
        current = load(uid) ?: newUser(uid, "")
        return current
    }

    private fun newUser(uid: String, name: String): UserData {
        val phone = CloudSync.phone
        val method = if (phone != null) "mobile" else "email"
        val value = phone ?: CloudSync.email ?: ""
        return UserData(Profile(uid, method, value, name, System.currentTimeMillis()))
    }

    /**
     * Call right after Firebase sign-in succeeds. Opens this farmer's data on the phone.
     * Records saved by the earlier phone-only version of the app (stored under
     * "mobile:<number>" or "email:<address>") are moved to the Firebase account.
     */
    fun signIn(method: String, value: String, name: String): UserData? {
        val uid = CloudSync.uid ?: return null
        var u = load(uid)
        if (u == null) {
            val legacyKey = "$method:$value"
            val legacy = load(legacyKey)
            u = if (legacy != null) {
                prefs.edit().remove(USER_PREFIX + legacyKey).apply()
                UserData(legacy.profile.copy(key = uid, method = method, value = value), legacy.daily, legacy.batches)
            } else {
                UserData(Profile(uid, method, value, name, System.currentTimeMillis()))
            }
        }
        if (name.isNotBlank()) u.profile.name = name
        current = u
        persist()
        return u
    }

    fun logout() {
        CloudSync.signOut()
        current = null
    }

    /** Saves the farmer's data on the phone and uploads the changes. Call after every change. */
    fun save() {
        val u = current ?: return
        version++
        persist()
        CloudSync.push(u, lang())
    }

    private fun persist() {
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

    // ---------- Cloud sync ----------

    fun addListener(l: () -> Unit) {
        listeners.add(l)
    }

    fun removeListener(l: () -> Unit) {
        listeners.remove(l)
    }

    /** Starts a download without waiting for it (used when the app opens). */
    fun syncInBackground() {
        scope.launch { syncFromCloud() }
    }

    /**
     * Downloads the farmer's records from Firestore and makes them the phone's copy.
     * The first time on a phone, records that exist only on the phone are kept and uploaded.
     * Returns false if it could not reach the cloud (the phone's copy is kept as it is).
     */
    suspend fun syncFromCloud(): Boolean {
        val u = user() ?: return false
        val uid = u.profile.key
        val firstTimeOnThisPhone = !CloudSync.hasSyncedBefore(uid)
        repeat(3) {
            val started = version
            val cloud = try {
                CloudSync.pull(u.profile)
            } catch (e: Exception) {
                Log.w(TAG, "Could not download records", e)
                return false
            }
            if (started != version) return@repeat // something was saved meanwhile: download again
            if (cloud == null) {
                // Nothing in the cloud yet (new account): upload what this phone has
                CloudSync.push(u, lang())
                return true
            }
            CloudSync.markSynced(cloud, lang())
            if (firstTimeOnThisPhone) keepLocalOnly(cloud, u)
            u.daily.clear()
            u.daily.putAll(cloud.daily)
            u.batches.clear()
            u.batches.addAll(cloud.batches)
            if (u.profile.name.isBlank()) u.profile.name = cloud.profile.name
            persist()
            CloudSync.push(u, lang()) // uploads anything kept from this phone
            listeners.toList().forEach { it() }
            return true
        }
        return false
    }

    /** Adds records that exist only on the phone into the downloaded data. */
    private fun keepLocalOnly(cloud: UserData, local: UserData) {
        for ((type, list) in local.daily) {
            val target = cloud.daily.getOrPut(type) { mutableListOf() }
            for (r in list) if (target.none { it.date == r.date }) target.add(r)
        }
        for (b in local.batches) if (cloud.batches.none { it.id == b.id }) cloud.batches.add(b)
    }

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

    /** Deletes all records on this phone and in the cloud (the account stays). */
    fun clearAll() {
        val u = user() ?: return
        u.daily.clear()
        u.batches.clear()
        save()
    }
}
