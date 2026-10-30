package com.hirephaze.steno.engine

import java.text.DateFormat
import java.util.Date

/** One shortcut -> expansion template pair, as the matcher sees it. */
data class PhraseRule(val shortcut: String, val expansion: String)

/** Result of a successful match: the field's new contents and caret position. */
data class Expansion(val newText: String, val newSelection: Int)

/** Values substituted for `{...}` placeholders; injectable so tests are deterministic. */
data class PlaceholderContext(
    val date: String,
    val time: String,
) {
    companion object {
        fun now(at: Date = Date()): PlaceholderContext = PlaceholderContext(
            date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(at),
            time = DateFormat.getTimeInstance(DateFormat.SHORT).format(at),
        )
    }
}

/**
 * Pure text-expansion core. Given what is currently in a field and where the
 * caret is, decide whether the text immediately before the caret ends with a
 * known shortcut and, if so, produce the rewritten field.
 *
 * Matching rules:
 *  - The longest matching shortcut wins (so `;addr2` beats `;addr`).
 *  - A shortcut that starts with a non-alphanumeric sigil (`;`, `/`, `:`…)
 *    needs no boundary before it — the sigil is the boundary.
 *  - A plain-word shortcut (`brb`) only fires when preceded by start-of-text
 *    or a non-alphanumeric character, so it never triggers inside a word.
 *
 * Deliberately has no Android dependencies so it is unit-tested on the JVM.
 */
object Expander {

    fun expand(
        text: String,
        selection: Int,
        rules: List<PhraseRule>,
        ctx: PlaceholderContext = PlaceholderContext.now(),
    ): Expansion? {
        if (rules.isEmpty()) return null
        val caret = selection.coerceIn(0, text.length)
        val before = text.substring(0, caret)

        val match = rules
            .asSequence()
            .filter { it.shortcut.isNotEmpty() && before.endsWith(it.shortcut) && boundaryOk(before, it.shortcut) }
            .maxByOrNull { it.shortcut.length }
            ?: return null

        val start = caret - match.shortcut.length
        val (resolved, caretInResolved) = resolveTemplate(match.expansion, ctx)
        val newText = text.substring(0, start) + resolved + text.substring(caret)
        val newSelection = start + (caretInResolved ?: resolved.length)
        return Expansion(newText, newSelection)
    }

    private fun boundaryOk(before: String, shortcut: String): Boolean {
        if (!shortcut.first().isLetterOrDigit()) return true // sigil-prefixed
        val posBefore = before.length - shortcut.length - 1
        if (posBefore < 0) return true // shortcut sits at the very start
        return !before[posBefore].isLetterOrDigit()
    }

    /**
     * Expands `{...}` placeholders. Returns the resolved string and, if a
     * `{cursor}` marker was present, the caret offset within it (the marker is
     * removed); otherwise null, meaning the caret goes to the end.
     */
    internal fun resolveTemplate(template: String, ctx: PlaceholderContext): Pair<String, Int?> {
        val withValues = template
            .replace("{date}", ctx.date)
            .replace("{time}", ctx.time)
            .replace("{nl}", "\n")
        val cursorIndex = withValues.indexOf("{cursor}")
        return if (cursorIndex >= 0) {
            withValues.replaceFirst("{cursor}", "") to cursorIndex
        } else {
            withValues to null
        }
    }
}
