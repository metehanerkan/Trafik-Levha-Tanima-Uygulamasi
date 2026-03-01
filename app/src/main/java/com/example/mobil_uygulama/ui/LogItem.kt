package com.example.mobil_uygulama.ui

import android.R
import android.graphics.Bitmap

data class LogItem(
    val label: String,
    val confidence: Double,
    val image: Bitmap,
    val timestamp: String
)

