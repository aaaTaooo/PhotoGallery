package com.example.photogallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.photogallery.ui.PhotoViewer
import com.example.photogallery.ui.theme.PhotoGalleryTheme

class PhotoViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoId = intent.getStringExtra("photo_id") ?: ""
        enableEdgeToEdge()
        setContent {
            PhotoGalleryTheme {
                val photoGalleryViewModel = remember { PhotoGalleryViewModel() }
                PhotoViewer(photoId = photoId, viewModel = photoGalleryViewModel)
            }
        }
    }
}

