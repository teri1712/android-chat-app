package com.decade.practice.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable


@Serializable
@Immutable

data class ImageSpec(
    val filename: String,
    val uri: String,
    val width: Int,
    val height: Int,
)