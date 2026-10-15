package com.personal.shortscounter.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Labels taken from a real debug log of the YouTube Shorts player. */
class FingerprintTest {
    private val short = listOf(
        "Go to channel @filmytrio",
        "Subscribe to @filmytrio.",
        "Sookhe pedh pe gulab #shorts",
        "John met Genelia first time | Force movie #shorts",
        "like this video along with 12 thousand other people",
        "View 51 comments",
        "Save",
        "Share this video",
        "Remix",
        "See more videos using this sound",
        "Video Progress",
    )

    @Test
    fun `keeps only identifying labels`() {
        assertEquals(
            "Go to channel @filmytrio | John met Genelia first time | Force movie #shorts | Sookhe pedh pe gulab #shorts",
            Fingerprint.from(short),
        )
    }

    @Test
    fun `changing counts and subscribe state keep the same fingerprint`() {
        val later = short.map {
            when {
                it.startsWith("like this") -> "like this video along with 13 thousand other people"
                it.startsWith("View 51") -> "View 52 comments"
                it.startsWith("Subscribe") -> "Unsubscribe from @filmytrio."
                else -> it
            }
        }
        assertEquals(Fingerprint.from(short), Fingerprint.from(later))
    }

    @Test
    fun `different Shorts differ`() {
        val other = listOf(
            "Go to channel @merimeow-k6l",
            "Perhaps, in that moment, she realized just how special",
            "View 1,220 comments",
        )
        assertNotEquals(Fingerprint.from(short), Fingerprint.from(other))
    }

    @Test
    fun `ad Short has a fingerprint`() {
        assertEquals("GoZayaan Ad", Fingerprint.from(listOf("GoZayaan Ad", "GoZayaan Ad", "Action menu", "View 1 comment")))
    }

    @Test
    fun `only generic labels give no fingerprint`() {
        assertNull(Fingerprint.from(listOf("Save", "Share this video", "0 minutes 3 seconds of 0 minutes 19 seconds")))
    }
}
