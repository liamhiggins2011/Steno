package com.hirephaze.steno.data

import android.content.Context
import com.hirephaze.steno.engine.PhraseRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        rules = dao.getAll().map { PhraseRule(it.shortcut, it.expansion) }
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
