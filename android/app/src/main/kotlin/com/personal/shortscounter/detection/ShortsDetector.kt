package com.personal.shortscounter.detection

/**
 * Decides whether the Short on screen should be counted. Pure logic with no
 * Android dependencies, so it can be unit-tested.
 *
 * A new fingerprint is only counted right after a swipe or when entering the
 * Shorts player. Fingerprint changes without a swipe (labels loading late) just
 * update the current Short.
 */
class ShortsDetector(private val recentCapacity: Int) {
    enum class Decision {
        COUNTED,
        SAME_SHORT,
        SEEN_RECENTLY,
        UPDATED_WITHOUT_SWIPE,
        NO_FINGERPRINT,
        NOT_IN_SHORTS,
    }

    private var inShorts = false
    private var armed = false
    private var current: String? = null

    /** Insertion-ordered, oldest first. */
    private val recent = LinkedHashSet<String>()

    fun onSwipe() {
        armed = true
    }

    fun onLeftShorts(): Decision {
        inShorts = false
        armed = false
        current = null
        return Decision.NOT_IN_SHORTS
    }

    fun onShortVisible(fingerprint: String?): Decision {
        if (!inShorts) {
            inShorts = true
            armed = true
        }
        if (fingerprint == null) return Decision.NO_FINGERPRINT
        if (fingerprint == current) {
            armed = false
            return Decision.SAME_SHORT
        }

        val afterSwipe = armed
        armed = false
        current = fingerprint
        val seen = fingerprint in recent
        remember(fingerprint)
        return when {
            !afterSwipe -> Decision.UPDATED_WITHOUT_SWIPE
            seen -> Decision.SEEN_RECENTLY
            else -> Decision.COUNTED
        }
    }

    private fun remember(fingerprint: String) {
        recent.remove(fingerprint)
        recent.add(fingerprint)
        if (recent.size > recentCapacity) recent.remove(recent.first())
    }
}
