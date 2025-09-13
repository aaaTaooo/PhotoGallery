package com.example.photogallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.photogallery.ui.PhotoViewer
import com.example.photogallery.ui.theme.PhotoGalleryTheme

class PhotoViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoId = intent.getStringExtra("photo_id") ?: ""
        enableEdgeToEdge()
        setContent {
            PhotoGalleryTheme {
                PhotoViewer(photoId = photoId)
            }
        }
    }
}

