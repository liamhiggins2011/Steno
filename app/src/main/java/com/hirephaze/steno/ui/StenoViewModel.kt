package com.hirephaze.steno.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hirephaze.steno.data.Phrase
import com.hirephaze.steno.data.PhraseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StenoViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PhraseRepository.get(app)

    val phrases: StateFlow<List<Phrase>> =
        repo.phrases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(shortcut: String, expansion: String, id: Long?) {
        viewModelScope.launch { repo.save(shortcut.trim(), expansion, id) }
    }

    fun delete(phrase: Phrase) {
        viewModelScope.launch { repo.delete(phrase) }
    }
}
