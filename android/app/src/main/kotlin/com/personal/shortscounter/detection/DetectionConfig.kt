package com.personal.shortscounter.detection

/**
 * Everything that depends on YouTube's internal layout lives here.
 * When YouTube changes its UI, turn on Debug mode in the app, check the logged
 * view IDs, and update this file.
 */
object DetectionConfig {
    /** The only app the accessibility service listens to. */
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    /**
     * View IDs (without the "com.google.android.youtube:id/" prefix). If any of
     * them is visible on screen, the Shorts player is open.
     */
    val SHORTS_CONTAINER_IDS = listOf(
        "reel_recycler",
        "reel_player_page_container",
        "reel_watch_player",
    )

    /** Wait this long after the last YouTube event before inspecting the screen. */
    const val SETTLE_DELAY_MS = 450L

    /** Counts reset at midnight in this time zone. */
    const val TIMEZONE = "Asia/Dhaka"

    fun fullId(id: String) = "$YOUTUBE_PACKAGE:id/$id"
}
