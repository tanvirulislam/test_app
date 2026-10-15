package com.personal.shortscounter.debug

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** Turns the visible part of an accessibility node tree into readable log text. */
object NodeDump {
    private const val MAX_LINES = 80
    private const val MAX_DEPTH = 40

    fun dump(root: AccessibilityNodeInfo): String {
        val out = StringBuilder()
        val bounds = Rect()
        var lines = 0

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (lines >= MAX_LINES || depth > MAX_DEPTH || !node.isVisibleToUser) return
            val id = node.viewIdResourceName?.substringAfter(":id/")
            val label = (node.text ?: node.contentDescription)?.oneLine(50)
            if (id != null || label != null) {
                node.getBoundsInScreen(bounds)
                out.append(" ".repeat(depth.coerceAtMost(20)))
                    .append(id ?: "-")
                    .append(" [").append(node.className?.toString()?.substringAfterLast('.') ?: "?").append("]")
                    .append(" y=").append(bounds.top).append("..").append(bounds.bottom)
                if (label != null) out.append(" \"").append(label).append('"')
                out.append('\n')
                lines++
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { visit(it, depth + 1) }
            }
        }

        visit(root, 0)
        if (lines >= MAX_LINES) out.append("… (truncated at $MAX_LINES lines)")
        return out.toString().trimEnd()
    }
}

fun CharSequence.oneLine(max: Int): String {
    val flat = toString().replace('\n', ' ').trim()
    return if (flat.length <= max) flat else flat.take(max) + "…"
}
