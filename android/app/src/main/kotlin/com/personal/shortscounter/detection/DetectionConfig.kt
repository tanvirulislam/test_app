package com.personal.shortscounter.detection

/**
 * Everything that depends on YouTube's internal layout lives here.
 * When YouTube changes its UI, turn on Debug mode in the app, check the logged
 * view IDs and labels, and update this file.
 */
object DetectionConfig {
    /** The only app the accessibility service listens to. */
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    /**
     * View IDs (without the "com.google.android.youtube:id/" prefix). If any of
     * them is visible on screen, the Shorts player is open. The first match is
     * also where the labels for the fingerprint are read, so keep the one that
     * wraps the whole Short (title, channel, buttons) first.
     */
    val SHORTS_CONTAINER_IDS = listOf(
        "reel_recycler",
        "reel_player_page_container",
    )

    /** A VIEW_SCROLLED event from one of these means you swiped between Shorts. */
    val SWIPE_SOURCE_IDS = setOf(
        "reel_player_underlay",
        "reel_recycler",
    )

    /**
     * Labels (text or content description) left out of a Short's fingerprint,
     * because they change while you watch (likes, comments, playback time,
     * subscribe state) or are the same on every Short. Matched case-insensitively.
     */
    val IGNORED_LABEL_PATTERNS = listOf(
        "^(un)?like",
        "^dislike",
        "^view .*comments?$",
        "^comments?$",
        "^(un)?subscribe",
        "^save$",
        "^share",
        "^remix",
        "^see more videos using this sound$",
        "^use template$",
        "^action menu$",
        "^video progress$",
        "^more actions$",
        "^search$",
        "^(play|pause)",
        "^\\d+ minutes? \\d+ seconds? of ",
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    /** Wait this long after the last YouTube event before inspecting the screen. */
    const val SETTLE_DELAY_MS = 450L

    /** How many recent Shorts to remember, so swiping back to one doesn't count it again. */
    const val RECENT_SHORTS_MEMORY = 30

    fun fullId(id: String) = "$YOUTUBE_PACKAGE:id/$id"
}
