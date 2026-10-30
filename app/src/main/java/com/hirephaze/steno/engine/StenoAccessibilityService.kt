package com.hirephaze.steno.engine

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hirephaze.steno.data.PhraseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The expansion engine's Android host. On every text change in an editable
 * field it reads the field, asks [Expander] whether a shortcut was just
 * completed, and if so rewrites the field with ACTION_SET_TEXT, then places
 * the caret with ACTION_SET_SELECTION.
 *
 * Works on API 26+. (A future refinement could inject via the API-34
 * AccessibilityService InputMethod for apps that ignore ACTION_SET_TEXT.)
 */
class StenoAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repo: PhraseRepository

    /** Suppresses the text-change event our own ACTION_SET_TEXT causes. */
    @Volatile private var lastInjectedText: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        repo = PhraseRepository.get(this)
        scope.launch { repo.refreshSnapshot() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        if (event.packageName == packageName) return // ignore our own UI

        val node = event.source ?: return
        try {
            if (!node.isEditable) return
            val text = node.text?.toString() ?: return
            if (text == lastInjectedText) return // our own injection echoing back

            val caret = node.textSelectionEnd.let { if (it in 0..text.length) it else text.length }
            val result = Expander.expand(text, caret, repo.rules) ?: return

            if (injectText(node, result.newText)) {
                lastInjectedText = result.newText
                setCaret(node, result.newSelection)
            }
        } finally {
            @Suppress("DEPRECATION")
            node.recycle()
        }
    }

    private fun injectText(node: AccessibilityNodeInfo, newText: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun setCaret(node: AccessibilityNodeInfo, position: Int) {
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, position)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, position)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
