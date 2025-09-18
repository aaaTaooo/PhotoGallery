package com.example.photogallery.ui

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory.Options
import androidx.activity.ComponentActivity
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun PhotoViewer(activity: ComponentActivity, photoUri: Uri, orientation: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }

    //get screen size
    val maxWidth = Resources.getSystem().displayMetrics.widthPixels
    val maxHeight = Resources.getSystem().displayMetrics.heightPixels

    //load image with screen size
    LaunchedEffect(photoUri, orientation) {
        bitmap = withContext(Dispatchers.IO) {
            decodeSampledBitmap(activity, photoUri, maxWidth, maxHeight, orientation)
        }
    }

    bitmap?.let { bitmap ->
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    //handle user gesture scale and pan
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale *= zoom
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    )
            )
        }
    }
}

//decode bitmap and rotate the image to display
private fun decodeSampledBitmap(
    activity: ComponentActivity,
    uri: Uri,
    maxWidth: Int,
    maxHeight: Int,
    orientation: Int
): Bitmap? {
    val options = Options().apply { inJustDecodeBounds = true }
    activity.contentResolver.openInputStream(uri)
        ?.use { BitmapFactory.decodeStream(it, null, options) }
    options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
    options.inJustDecodeBounds = false

    val bitmap = activity.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return null

    //return a rotated bitmap
    return rotateBitmap(bitmap, orientation)
}

//Calculate the sample size
private fun calculateInSampleSize(options: Options, requestedWidth: Int, requestedHeight: Int): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > requestedHeight || width > requestedWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= requestedHeight && halfWidth / inSampleSize >= requestedWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

//adjust the orientation of displaying images
fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    if (orientation == 0) return bitmap
    val matrix = Matrix().apply { postRotate(orientation.toFloat())}
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}