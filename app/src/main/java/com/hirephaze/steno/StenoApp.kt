package com.hirephaze.steno

import android.app.Application
import com.hirephaze.steno.data.PhraseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StenoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch { PhraseRepository.get(this@StenoApp).seedDefaultsIfEmpty() }
    }
}
