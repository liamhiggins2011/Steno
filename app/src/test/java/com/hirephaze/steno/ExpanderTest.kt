package com.hirephaze.steno

import com.hirephaze.steno.engine.Expander
import com.hirephaze.steno.engine.PhraseRule
import com.hirephaze.steno.engine.PlaceholderContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpanderTest {

    private val ctx = PlaceholderContext(date = "Oct 28, 2026", time = "10:30 AM")
    private val rules = listOf(
        PhraseRule(";brb", "be right back"),
        PhraseRule(";addr", "123 Example St"),
        PhraseRule(";addr2", "456 Second Ave"),
        PhraseRule("btw", "by the way"),
    )

    @Test
    fun `sigil shortcut expands at caret`() {
        val r = Expander.expand(";brb", 4, rules, ctx)!!
        assertEquals("be right back", r.newText)
        assertEquals("be right back".length, r.newSelection)
    }

    @Test
    fun `expansion keeps surrounding text and places caret`() {
        val text = "ok ;brb"
        val r = Expander.expand(text, text.length, rules, ctx)!!
        assertEquals("ok be right back", r.newText)
        assertEquals("ok be right back".length, r.newSelection)
    }

    @Test
    fun `longest matching shortcut wins`() {
        val r = Expander.expand(";addr2", 6, rules, ctx)!!
        assertEquals("456 Second Ave", r.newText)
    }

    @Test
    fun `word shortcut needs a boundary before it`() {
        // "abtw" should NOT expand btw because 'a' precedes it.
        assertNull(Expander.expand("abtw", 4, rules, ctx))
        // " btw" should expand.
        val r = Expander.expand("x btw", 5, rules, ctx)!!
        assertEquals("x by the way", r.newText)
    }

    @Test
    fun `no match returns null`() {
        assertNull(Expander.expand("hello", 5, rules, ctx))
    }

    @Test
    fun `caret only considers text before it`() {
        // Caret is in the middle; trailing ";brb" is after the caret, so no match.
        assertNull(Expander.expand("a;brb", 1, rules, ctx))
    }

    @Test
    fun `placeholders resolve date newline and cursor`() {
        val r = Expander.expand(
            ";x", 2,
            listOf(PhraseRule(";x", "Date: {date}{nl}{cursor}end")),
            ctx,
        )!!
        assertEquals("Date: Oct 28, 2026\nend", r.newText)
        // caret sits where {cursor} was: right after the newline.
        assertEquals("Date: Oct 28, 2026\n".length, r.newSelection)
    }
}
