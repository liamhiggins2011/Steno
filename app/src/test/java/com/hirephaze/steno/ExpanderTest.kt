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
    fun `a phrase field splits into one rule per pipe-separated variation`() {
        val built = PhraseRule.forPhrase("phoneno|Phoneno|pno", "215-555-0199")
        assertEquals(
            listOf(
                PhraseRule("phoneno", "215-555-0199"),
                PhraseRule("Phoneno", "215-555-0199"),
                PhraseRule("pno", "215-555-0199"),
            ),
            built,
        )
    }

    @Test
    fun `forPhrase trims blanks and drops empty and duplicate variations`() {
        val built = PhraseRule.forPhrase(" brb | brb ||", "be right back")
        assertEquals(listOf(PhraseRule("brb", "be right back")), built)
    }

    @Test
    fun `plain field with no separator yields a single rule`() {
        assertEquals(
            listOf(PhraseRule("brb", "be right back")),
            PhraseRule.forPhrase("brb", "be right back"),
        )
    }

    @Test
    fun `any variation of a shortcut expands to the same text`() {
        val phoneRules = PhraseRule.forPhrase("phoneno|Phoneno|pno", "215-555-0199")
        for (typed in listOf("phoneno", "Phoneno", "pno")) {
            val r = Expander.expand(typed, typed.length, phoneRules, ctx)!!
            assertEquals("expanding \"$typed\"", "215-555-0199", r.newText)
        }
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
