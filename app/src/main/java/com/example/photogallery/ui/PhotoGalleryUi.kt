package com.example.photogallery.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.example.photogallery.MAXCOLS
import com.example.photogallery.MINCOLS
import com.example.photogallery.PhotoGalleryViewModel
import com.example.photogallery.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Popup message - Are you sure
@Composable
fun SureDialog(modifier: Modifier = Modifier, onConfirmation: () -> Unit, show: MutableState<Boolean>) {
    if (show.value) {
        AlertDialog(
            modifier = modifier,
            text = { Text(text = stringResource(R.string.are_you_sure)) },
            onDismissRequest = { show.value = false },
            confirmButton = {
                TextButton(onClick = {
                    onConfirmation()
                    show.value = false
                }) { Text(stringResource(R.string.yes))} },
            dismissButton = {
                TextButton(onClick = { show.value = false }) { Text(stringResource(R.string.no)) }
            }
        )
    }
}
//Lazy Vertical Grid Photo Gallery with Gesture implementation
@Composable
fun PhotoGallery(modifier: Modifier = Modifier, viewModel: PhotoGalleryViewModel) {
    val showDialog = rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var zoom by remember { mutableFloatStateOf(1f) }
    val uiState by viewModel.uiState.collectAsState()

    SureDialog(modifier = modifier,{
        scope.launch {
            delay(1000)
        }
    }, showDialog)
    Scaffold(
        modifier = modifier,
        topBar = { AppBar(viewModel, showDialog, Modifier) },
    ) { innerPadding ->
        Box(
            Modifier.padding(innerPadding).fillMaxSize()
                .graphicsLayer{scaleX = zoom; scaleY = zoom}
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        do{
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1f) {
                                zoom *= zoomChange
                                if(zoom!=0f) {
                                    val cols = min(max((uiState.columns / zoom).roundToInt(), MINCOLS), MAXCOLS)
                                    if (cols != uiState.columns) {
                                        viewModel.updateColumns(cols)
                                        zoom =1f
                                    }
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        zoom = 1f
                    }
                }) {
            LazyVerticalGrid( columns = GridCells.Fixed(uiState.columns)) {
                items(uiState.photos.size) { index ->
                    //Under develop
                }
            }
        }
    }
}

//Top menu bar with app name and refresh button
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(viewModel: PhotoGalleryViewModel, showDialog: MutableState<Boolean>, modifier: Modifier) {
    var showDropDownMenu by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

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
                        showDialog.value = true
                    }
                )
            }
        }
    )
}