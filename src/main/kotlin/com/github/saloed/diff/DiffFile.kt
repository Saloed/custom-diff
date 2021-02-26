package com.github.saloed.diff

import kotlinx.serialization.Serializable

@Serializable
data class DiffFile(val title: String, val contentLines: List<String>)
