package com.github.saloed.diff

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TwoWayDiff(val left: DiffFile, val right: DiffFile, val diff: List<Diff>) {

    fun saveToJson() = Json.encodeToString(this)

    companion object {
        fun loadFromJson(data: String) = Json.decodeFromString<TwoWayDiff>(data)
    }
}
