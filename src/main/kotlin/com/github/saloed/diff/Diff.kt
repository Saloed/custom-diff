package com.github.saloed.diff

import kotlinx.serialization.Serializable

@Serializable
enum class DiffType {
    EQUAL, INSERT, DELETE
}

@Serializable
data class Diff(val type: DiffType, val left: DiffRange, val right: DiffRange) {
    fun isCorrectInMode(mode: DiffMode) = left.isCorrectInMode(mode) && right.isCorrectInMode(mode)
}
