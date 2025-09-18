package com.example.photogallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.photogallery.ui.PhotoViewer
import com.example.photogallery.ui.theme.PhotoGalleryTheme
import androidx.core.net.toUri

class PhotoViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoUriString = intent .getStringExtra("photo_uri") ?: ""
        val photoUri = photoUriString.toUri()
        val photoOrientation = intent.getIntExtra("photo_orientation", 0)
        enableEdgeToEdge()
        setContent {
            PhotoGalleryTheme {
                PhotoViewer(activity = this, photoUri = photoUri, orientation = photoOrientation)
            }
        }
    }
}

