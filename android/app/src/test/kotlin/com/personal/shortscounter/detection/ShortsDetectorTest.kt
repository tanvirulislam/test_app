package com.personal.shortscounter.detection

import com.personal.shortscounter.detection.ShortsDetector.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortsDetectorTest {
    private val detector = ShortsDetector(recentCapacity = 3)

    private fun swipeTo(fingerprint: String?): Decision {
        detector.onSwipe()
        return detector.onShortVisible(fingerprint)
    }

    @Test
    fun `first Short after entering is counted`() {
        assertEquals(Decision.COUNTED, detector.onShortVisible("a"))
    }

    @Test
    fun `each swipe to a new Short is counted once`() {
        detector.onShortVisible("a")
        assertEquals(Decision.COUNTED, swipeTo("b"))
        assertEquals(Decision.SAME_SHORT, detector.onShortVisible("b"))
        assertEquals(Decision.COUNTED, swipeTo("c"))
    }

    @Test
    fun `swiping back to a recent Short is not counted`() {
        detector.onShortVisible("a")
        swipeTo("b")
        assertEquals(Decision.SEEN_RECENTLY, swipeTo("a"))
        assertEquals(Decision.SEEN_RECENTLY, swipeTo("b"))
    }

    @Test
    fun `swipe that snaps back to the same Short is not counted`() {
        detector.onShortVisible("a")
        assertEquals(Decision.SAME_SHORT, swipeTo("a"))
    }

    @Test
    fun `label change without a swipe updates without counting`() {
        detector.onShortVisible("a")
        assertEquals(Decision.UPDATED_WITHOUT_SWIPE, detector.onShortVisible("a + late label"))
        assertEquals(Decision.COUNTED, swipeTo("b"))
        assertEquals(Decision.SEEN_RECENTLY, swipeTo("a + late label"))
        assertEquals(Decision.SEEN_RECENTLY, swipeTo("a"))
    }

    @Test
    fun `missing fingerprint keeps the swipe pending`() {
        detector.onShortVisible("a")
        assertEquals(Decision.NO_FINGERPRINT, swipeTo(null))
        assertEquals(Decision.COUNTED, detector.onShortVisible("b"))
    }

    @Test
    fun `returning to the same Short after leaving Shorts is not counted`() {
        detector.onShortVisible("a")
        detector.onLeftShorts()
        assertEquals(Decision.SEEN_RECENTLY, detector.onShortVisible("a"))
        detector.onLeftShorts()
        assertEquals(Decision.COUNTED, detector.onShortVisible("b"))
    }

    @Test
    fun `old Shorts fall out of the recent memory`() {
        detector.onShortVisible("a")
        swipeTo("b")
        swipeTo("c")
        swipeTo("d")
        assertEquals(Decision.COUNTED, swipeTo("a"))
    }
}
