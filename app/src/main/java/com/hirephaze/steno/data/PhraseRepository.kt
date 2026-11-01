package com.hirephaze.steno.data

import android.content.Context
import com.hirephaze.steno.engine.PhraseRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth for phrases. The UI observes [phrases] reactively;
 * the accessibility service reads [rules], a plain in-memory snapshot that is
 * cheap to touch on every keystroke and kept current by [refreshSnapshot].
 */
class PhraseRepository private constructor(context: Context) {

    private val dao = StenoDatabase.get(context).phraseDao()

    val phrases: Flow<List<Phrase>> = dao.observeAll()

    @Volatile
    var rules: List<PhraseRule> = emptyList()
        private set

    private val _snapshotReady = MutableStateFlow(false)
    val snapshotReady = _snapshotReady.asStateFlow()

    suspend fun refreshSnapshot() {
        rules = dao.getAll().flatMap { PhraseRule.forPhrase(it.shortcut, it.expansion) }
        _snapshotReady.value = true
    }

    suspend fun save(shortcut: String, expansion: String, id: Long?) {
        val now = System.currentTimeMillis()
        if (id == null || id == 0L) {
            dao.insert(Phrase(shortcut = shortcut, expansion = expansion, createdAt = now, updatedAt = now))
        } else {
            dao.update(Phrase(id = id, shortcut = shortcut, expansion = expansion, updatedAt = now))
        }
        refreshSnapshot()
    }

    suspend fun delete(phrase: Phrase) {
        dao.delete(phrase)
        refreshSnapshot()
    }

    /**
     * Re-inserts a phrase removed by [delete], keeping its original id so an
     * undo restores it exactly. Backs the "Undo" action after a delete.
     */
    suspend fun restore(phrase: Phrase) {
        dao.insert(phrase)
        refreshSnapshot()
    }

    /** Serializes every phrase to a human-readable JSON backup. */
    suspend fun exportJson(): String {
        val arr = JSONArray()
        dao.getAll().forEach { p ->
            arr.put(JSONObject().put("shortcut", p.shortcut).put("expansion", p.expansion))
        }
        return JSONObject()
            .put("app", "steno")
            .put("version", 1)
            .put("phrases", arr)
            .toString(2)
    }

    /**
     * Restores phrases from a backup produced by [exportJson]. Accepts either
     * that object form or a bare JSON array of {shortcut, expansion}. Existing
     * shortcuts are overwritten; when [replace] is true the table is cleared
     * first. Returns how many phrases were imported.
     */
    suspend fun importJson(text: String, replace: Boolean): Int {
        val arr = if (text.trimStart().startsWith("[")) {
            JSONArray(text)
        } else {
            JSONObject(text).optJSONArray("phrases") ?: JSONArray()
        }
        if (replace) dao.deleteAll()
        var imported = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val shortcut = o.optString("shortcut").trim()
            val expansion = o.optString("expansion")
            if (shortcut.isNotEmpty() && expansion.isNotEmpty()) {
                dao.insert(Phrase(shortcut = shortcut, expansion = expansion))
                imported++
            }
        }
        refreshSnapshot()
        return imported
    }

    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() == 0) {
            DEFAULTS.forEach { (s, e) ->
                dao.insert(Phrase(shortcut = s, expansion = e))
            }
        }
        refreshSnapshot()
    }

    companion object {
        @Volatile private var instance: PhraseRepository? = null

        fun get(context: Context): PhraseRepository =
            instance ?: synchronized(this) {
                instance ?: PhraseRepository(context).also { instance = it }
            }

        private val DEFAULTS = listOf(
            ";brb" to "be right back",
            ";omw" to "on my way",
            ";date" to "{date}",
            ";sig" to "Sent from Steno,{nl}Liam",
            ";addr" to "123 Example St{nl}Philadelphia, PA",
        )
    }
}
