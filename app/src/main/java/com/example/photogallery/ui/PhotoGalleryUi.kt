package com.example.photogallery.ui

import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.photogallery.MAXCOLS
import com.example.photogallery.MINCOLS
import com.example.photogallery.MainActivity
import com.example.photogallery.PhotoGalleryViewModel
import com.example.photogallery.PhotoViewerActivity
import com.example.photogallery.R

//Lazy Vertical Grid Photo Gallery with Gesture implementation
@Composable
fun PhotoGallery(viewModel: PhotoGalleryViewModel, activity: MainActivity) {
    val uiState by viewModel.uiState.collectAsState()
    var zoom by remember { mutableFloatStateOf(1f) }

    Scaffold(
        topBar = { AppBar(viewModel, activity, Modifier) }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .graphicsLayer { scaleX = zoom; scaleY = zoom }
                .pointerInput(Unit) {
                    //Gesture for zoom in and zoom out feature for displaying different columns
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1f) {
                                zoom *= zoomChange
                                when {
                                    zoom > 1.5f && uiState.columns < MAXCOLS -> {
                                        viewModel.updateColumns(uiState.columns + 1)
                                        zoom = 1f
                                    }
                                    zoom < 0.5f && uiState.columns > MINCOLS -> {
                                        viewModel.updateColumns(uiState.columns - 1)
                                        zoom = 1f
                                    }
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        zoom = 1f
                    }
                }
        ) {
            //LazyVerticalGrid of thumbnails with a LaunchedEffect to load photos
            LazyVerticalGrid(
                columns = GridCells.Fixed(uiState.columns),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.photos.size) { index ->
                    val photo = uiState.photos[index]
                    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                    var scale by remember { mutableFloatStateOf(0f) }

                    //using a coroutine with the IO dispatcher to load thumbnails
                    LaunchedEffect(photo.id) {
                        bitmap = viewModel.loadThumbnail(activity, photo, 200, 160)
                        scale = 1f
                    }
                    //float animation
                    val animatedScale by animateFloatAsState(
                        targetValue = scale,
                        animationSpec = tween(durationMillis = 300)
                    )

                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1.25f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                //load thumbnails animation
                                .graphicsLayer{
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                                .clickable{
                                    val uri = ContentUris.withAppendedId(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        photo.id.toLong()
                                    )
                                    val intent = Intent(activity, PhotoViewerActivity::class.java).apply {
                                        putExtra("photo_uri",uri.toString())
                                        putExtra("photo_orientation", photo.orientation)
                                    }
                                    activity.startActivity(intent)
                                },
                            contentScale = ContentScale.Crop
                        )
                    } ?: Box(
                        Modifier
                            .aspectRatio(1.25f)  //thumbnails ratio
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

//Top menu bar with app name and refresh button
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(viewModel: PhotoGalleryViewModel, activity: MainActivity, modifier: Modifier) {
    var showDropDownMenu by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        title = { Text(text = "Photo Gallery") },
        actions = {
            IconButton(onClick = { showDropDownMenu = true }) {
                Icon(Icons.Filled.MoreVert, null)
            }
            DropdownMenu(
                expanded = showDropDownMenu,
                onDismissRequest = { showDropDownMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.refresh)) },
                    leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                    onClick = {
                        showDropDownMenu = false
                        viewModel.refreshPhotos(activity)
                    }
                )
            }
        }
    )
}