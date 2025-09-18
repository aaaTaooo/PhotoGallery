package com.example.photogallery

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.collection.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory.Options
import android.graphics.Matrix
import androidx.core.graphics.scale

const val MINCOLS=1
const val MAXCOLS=3
class PhotoGalleryViewModel : ViewModel() {
    //private mutable Ui state for updating info
    private val _uiState = MutableStateFlow(PhotoGalleryUiState())
    //public immutable Ui state
    val uiState: StateFlow<PhotoGalleryUiState> = _uiState.asStateFlow()

    //LRU cache for storing Bitmap
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }
    fun getBitmap(key: String): Bitmap? {
        return memoryCache[key]
    }

    //avoid bitmap stored with repeat key
    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) memoryCache.put(key, bitmap)
    }

    // Update the number of grid columns in Ui state
    fun updateColumns(newCols: Int) {
        _uiState.update { currentState -> currentState.copy(columns = newCols) }
    }

    // Reload photo list from MediaStore
    fun refreshPhotos(activity: MainActivity) {
        _uiState.update { currentState -> currentState.copy(photos = emptyList(), isLoading = true) }
        loadPhotos(activity)
    }

    //Load photo data from MediaStore and update mutable Ui state with photo list
    fun loadPhotos(activity: MainActivity) {
        _uiState.update { currentState -> currentState.copy(isLoading = true) }

        //Load Photos in the IO Dispatchers
        viewModelScope.launch(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )

            val photos = mutableListOf<Photo>()
            val cursor = activity.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            cursor?.use { cursor ->
                val idColumns = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val orientationColumns =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idColumns)
                    val orientation = cursor.getInt(orientationColumns)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    photos.add(Photo(id, orientation, width, height))

                }
            }
            _uiState.update { cursor -> cursor.copy(photos = photos) }
        }
    }

    //Load bitmap thumbnail for the given photo from MediaStore
    suspend fun loadThumbnail(
        activity: MainActivity,
        photo: Photo,
        requestedWidth: Int,
        requestedHeight: Int
    ): Bitmap? {
        //check the bitmap key first
        val key = photo.id
        getBitmap(key)?.let { return it }

        val uri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            photo.id.toLong()
        )

        val bitmap = withContext(Dispatchers.IO) {
            decodeSampledBitmapFromUri(activity, uri, requestedWidth, requestedHeight, photo.orientation)
        }

        bitmap?.let { putBitmap(key, it) }
        return bitmap
    }

    //decode bitmap and cut the size to fit thumbnail size
    private fun decodeSampledBitmapFromUri(
        activity: MainActivity,
        uri: Uri,
        requestedWidth: Int,
        requestedHeight: Int,
        orientation: Int
    ): Bitmap? {
        val options = Options().apply { inJustDecodeBounds = true }
        activity.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = calculateInSampleSize(options, requestedWidth, requestedHeight)
        options.inJustDecodeBounds = false
        val bitmap = activity.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        //keep the original ratio of the images in the thumbnails
        val oriented = rotateBitmap(bitmap, orientation)
        val sourceRatio = oriented.width.toFloat() / oriented.height.toFloat()
        val scaled: Bitmap
        if (sourceRatio > 1.25) {
            //if the image is wider than the thumbnail size
            val newHeight = requestedHeight
            val newWidth = (newHeight * sourceRatio).toInt()
            scaled = oriented.scale(newWidth, newHeight)
        } else {
            //if the image is higher than the thumbnail size
            val newWidth = requestedWidth
            val newHeight = (newWidth / sourceRatio).toInt()
            scaled = oriented.scale(newWidth, newHeight)
        }
        val x = (scaled.width - requestedWidth) / 2
        val y = (scaled.height - requestedHeight) / 2
        return Bitmap.createBitmap(scaled, x, y, requestedWidth , requestedHeight)
    }

    //Calculate the sample size to downsize large images
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

    //adjust the orientation of displaying images in thumbnails
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}