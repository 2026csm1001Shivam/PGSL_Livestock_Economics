package com.gadvasu.livestockapp.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.data.Profile
import com.gadvasu.livestockapp.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.floor

/**
 * Keeps the farmer's data in Cloud Firestore in step with the copy on the phone.
 *
 * push(): after every save, uploads only the documents that changed (and deletes removed ones).
 *         It remembers a fingerprint of each document it sent, so unchanged records aren't re-sent.
 *         Firestore keeps its own offline queue: without internet, writes wait on the phone and
 *         upload automatically later, even after the app is closed.
 * pull(): downloads all of the farmer's documents (used at login and when the app starts).
 */
object CloudSync {

    private const val TAG = "CloudSync"
    private const val PREFS = "gadvasu_sync"
    private const val UPDATED_AT = "updatedAt"
    private const val CREATED_AT = "createdAt"

    private lateinit var prefs: SharedPreferences

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    // ---------------- Firebase login state ----------------

    /** Firebase user ID of the signed-in farmer, or null. */
    val uid: String? get() = auth.currentUser?.uid

    /** Phone number of the signed-in farmer without "+91", if they logged in with mobile. */
    val phone: String? get() = auth.currentUser?.phoneNumber?.removePrefix("+91")?.takeIf { it.isNotEmpty() }

    val email: String? get() = auth.currentUser?.email?.takeIf { it.isNotEmpty() }

    fun signOut() = auth.signOut()

    // ---------------- fingerprints of what was last sent ----------------

    private fun syncedKey(uid: String) = "synced:$uid"

    fun hasSyncedBefore(uid: String): Boolean = prefs.contains(syncedKey(uid))

    private fun loadSynced(uid: String): MutableMap<String, String> {
        val out = HashMap<String, String>()
        val raw = prefs.getString(syncedKey(uid), null) ?: return out
        try {
            val o = JSONObject(raw)
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = o.getString(k)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read sync state", e)
        }
        return out
    }

    private fun storeSynced(uid: String, m: Map<String, String>) {
        val o = JSONObject()
        for ((k, v) in m) o.put(k, v)
        prefs.edit().putString(syncedKey(uid), o.toString()).apply()
    }

    // ---------------- documents ----------------

    /** Every Firestore document for this farmer: path -> fields (no timestamps). */
    private fun documents(uid: String, u: UserData, language: String): LinkedHashMap<String, Map<String, Any?>> {
        val docs = LinkedHashMap<String, Map<String, Any?>>()
        val userPath = "${Schema.USERS}/$uid"
        docs[userPath] = u.profile.toUserFields(language)
        for ((type, list) in u.daily) {
            for (r in list) {
                docs["$userPath/${Schema.DAILY_RECORDS}/${Schema.DailyDoc.id(type, r.date)}"] = r.toDailyFields(type)
            }
        }
        for (b in u.batches) {
            val batchPath = "$userPath/${Schema.BATCHES}/${b.id}"
            docs[batchPath] = b.toBatchFields()
            for (e in b.records) docs["$batchPath/${Schema.EXPENSES}/${e.id}"] = e.toExpenseFields()
        }
        return docs
    }

    /** Uploads documents that changed since the last push and deletes ones that were removed. */
    fun push(u: UserData, language: String) {
        val uid = uid ?: return
        if (u.profile.key != uid) return
        val synced = loadSynced(uid)
        val docs = documents(uid, u, language)
        val ops = mutableListOf<(WriteBatch) -> Unit>()

        for ((path, fields) in docs) {
            val fp = fingerprint(fields)
            if (synced[path] == fp) continue
            val data = HashMap<String, Any?>(fields)
            data[UPDATED_AT] = FieldValue.serverTimestamp()
            if (!synced.containsKey(path)) data[CREATED_AT] = FieldValue.serverTimestamp()
            val ref = db.document(path)
            ops.add { wb -> wb.set(ref, data, SetOptions.merge()) }
            synced[path] = fp
        }
        for (path in synced.keys.toList()) {
            if (!docs.containsKey(path)) {
                val ref = db.document(path)
                ops.add { wb -> wb.delete(ref) }
                synced.remove(path)
            }
        }
        if (ops.isEmpty()) return
        storeSynced(uid, synced)

        // One write batch holds up to 500 operations
        ops.chunked(400).forEach { chunk ->
            val wb = db.batch()
            chunk.forEach { it(wb) }
            wb.commit().addOnFailureListener { e ->
                // Forget what was sent so the next save uploads everything again
                Log.w(TAG, "Upload failed", e)
                prefs.edit().remove(syncedKey(uid)).apply()
            }
        }
    }

    /**
     * Downloads all of the farmer's data. Returns null if there is nothing in the cloud yet.
     * Throws if the phone is offline and Firestore has no saved copy.
     */
    suspend fun pull(local: Profile): UserData? {
        val uid = uid ?: return null
        val userRef = db.collection(Schema.USERS).document(uid)
        val userSnap = userRef.get().await()
        if (!userSnap.exists()) return null

        val u = UserData(userSnap.toProfile(uid, local))
        for (type in Ent.DAILY) u.daily[type] = mutableListOf()
        val daily = userRef.collection(Schema.DAILY_RECORDS).get().await()
        for (d in daily.documents) {
            val type = d.getString(Schema.DailyDoc.ENTERPRISE) ?: continue
            u.daily.getOrPut(type) { mutableListOf() }.add(d.toDailyRecord())
        }
        val batches = userRef.collection(Schema.BATCHES).get().await()
        for (b in batches.documents) {
            val expenses = b.reference.collection(Schema.EXPENSES).get().await().documents.map { it.toBatchExpense() }
            u.batches.add(b.toBatch(expenses))
        }
        return u
    }

    /** Records [u] (just downloaded) as already uploaded, so it isn't sent back. */
    fun markSynced(u: UserData, language: String) {
        val uid = uid ?: return
        storeSynced(uid, documents(uid, u, language).mapValues { fingerprint(it.value) })
    }

    // ---------------- fingerprint ----------------

    /** Same data always gives the same text, whatever the key order or number type. */
    private fun canonical(v: Any?): String = when (v) {
        null -> "null"
        is Map<*, *> -> v.entries.sortedBy { it.key.toString() }
            .joinToString(",", "{", "}") { JSONObject.quote(it.key.toString()) + ":" + canonical(it.value) }
        is List<*> -> v.joinToString(",", "[", "]") { canonical(it) }
        is Number -> {
            val d = v.toDouble()
            if (d == floor(d) && abs(d) < 1e15) d.toLong().toString() else d.toString()
        }
        is String -> JSONObject.quote(v)
        else -> JSONObject.quote(v.toString())
    }

    private fun fingerprint(fields: Map<String, Any?>): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(canonical(fields).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
