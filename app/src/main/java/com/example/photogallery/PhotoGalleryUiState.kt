package com.example.photogallery

import android.graphics.Bitmap

data class PhotoGalleryUiState(
    val photos: List<Photo> = emptyList(),
    val thumbnails: Map<String, Bitmap> = emptyMap(),
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val columns: Int = 3
)