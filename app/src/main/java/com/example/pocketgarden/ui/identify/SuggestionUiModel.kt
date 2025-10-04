package com.example.pocketgarden.ui.identify

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SuggestionUiModel(
    val name: String,
    val probability: Double,
    val commonNames: List<String>,
    val imageUrl: String?
) : Parcelable
