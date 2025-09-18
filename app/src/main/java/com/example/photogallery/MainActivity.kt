package com.example.photogallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.photogallery.ui.PhotoGallery
import com.example.photogallery.ui.theme.PhotoGalleryTheme

class MainActivity : ComponentActivity() {
    private val photoGalleryViewModel: PhotoGalleryViewModel by viewModels()
    private var permissionGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissionAndShowGallery()
    }

    override fun onResume() {
        super.onResume()
        if (permissionGranted) {
            photoGalleryViewModel.refreshPhotos(this)
        }
    }

    private fun checkPermissionAndShowGallery() {
        //check the sdk version for different permission
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        //if permission granted show photo gallery, else request permission
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true
        } else {
            requestPermission.launch(permission)
        }

        setContent {
            PhotoGalleryTheme {
                if (permissionGranted) {
                    LaunchedEffect(Unit) {
                        photoGalleryViewModel.loadPhotos(this@MainActivity)
                    }
                    PhotoGallery(viewModel = photoGalleryViewModel, activity = this@MainActivity )
                } else {
                    Text("Need permission to view photo gallery")
                }
            }
        }
    }

    //get permission from user then renew the permission flag and trigger photo gallery display
    val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {granted ->
        permissionGranted = granted
    }
}