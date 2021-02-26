package com.github.saloed.diff

import kotlinx.serialization.Serializable

@Serializable
data class DiffRange(val startLine: Int, val startLineOffset: Int, val endLine: Int, val endLineOffset: Int) {
    fun apply(contentLines: List<String>): String {
        if (startLine == endLine) return contentLines[startLine].substring(startLineOffset, endLineOffset)
        val lines = (startLine..endLine).map { contentLines[it] }.toMutableList()
        lines[0] = lines[0].substring(startLineOffset)
        lines[lines.lastIndex] = lines[lines.lastIndex].substring(0, endLineOffset)
        return lines.joinToString("\n")
    }
}
