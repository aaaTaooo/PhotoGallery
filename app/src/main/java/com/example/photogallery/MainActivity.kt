package com.example.photogallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.example.photogallery.ui.PhotoGallery
import com.example.photogallery.ui.theme.PhotoGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val requestPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            setContent {
                PhotoGalleryTheme {
                    if (granted) {
                        val photoGalleryViewModel = remember { PhotoGalleryViewModel() }
                        LaunchedEffect(Unit) {
                            photoGalleryViewModel.loadPhotos(this@MainActivity)
                        }
                        PhotoGallery(viewModel = photoGalleryViewModel, activity = this@MainActivity )
                    } else {
                        Text("Require permission to view Photos")
                    }
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(permission)
        } else {
            requestPermission.launch(permission)
        }
    }
}