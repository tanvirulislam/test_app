package com.personal.shortscounter.detection

import android.view.accessibility.AccessibilityNodeInfo

/** Builds a string that identifies the Short currently on screen. */
object Fingerprint {
    private const val MAX_DEPTH = 40
    private const val MAX_LABEL_LENGTH = 200

    /** Visible text and content descriptions under [container]. */
    fun collectLabels(container: AccessibilityNodeInfo): List<String> {
        val labels = mutableListOf<String>()

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > MAX_DEPTH || !node.isVisibleToUser) return
            listOf(node.text, node.contentDescription).forEach { label ->
                if (!label.isNullOrBlank()) labels += label.toString()
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { visit(it, depth + 1) }
            }
        }

        visit(container, 0)
        return labels
    }

    /**
     * Drops ignored labels and joins the rest in a stable order.
     * Returns null when nothing identifying is left.
     */
    fun from(labels: Iterable<String>): String? = labels
        .map { it.replace('\n', ' ').trim().take(MAX_LABEL_LENGTH) }
        .filter { label -> label.isNotEmpty() && DetectionConfig.IGNORED_LABEL_PATTERNS.none { it.containsMatchIn(label) } }
        .toSortedSet()
        .joinToString(" | ")
        .ifEmpty { null }
}
