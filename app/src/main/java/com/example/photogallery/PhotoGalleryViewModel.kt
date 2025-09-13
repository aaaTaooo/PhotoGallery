package com.example.photogallery

import android.R.attr.orientation
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

const val MINCOLS=1
const val MAXCOLS=3
class PhotoGalleryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoGalleryUiState())
    val uiState: StateFlow<PhotoGalleryUiState> = _uiState.asStateFlow()

    //Caching Bitmap
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }
    fun getBitmap(key: String): Bitmap? {
        return memoryCache.get(key)
    }
    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) memoryCache.put(key, bitmap)
    }

    fun updateColumns(newCols: Int) {
        _uiState.update { currentState -> currentState.copy(columns = newCols) }
    }

    fun refreshPhotos(activity: MainActivity) {
        loadPhotos(activity)
    }

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
            //val thumbnails = mutableMapOf<String, Bitmap>()
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

    suspend fun loadThumbnail(
        activity: MainActivity,
        photo: Photo,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val key = photo.id
        getBitmap(key)?.let { return it }

        val uri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            photo.id.toLong()
        )

        val bitmap = withContext(Dispatchers.IO) {
            decodeSampledBitmapFromUri(activity, uri, reqWidth, reqHeight, photo.orientation)
        }

        bitmap?.let { putBitmap(key, it) }
        return bitmap
    }

    private fun decodeSampledBitmapFromUri(
        activity: MainActivity,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int,
        orientation: Int
    ): Bitmap? {
        val options = Options().apply { inJustDecodeBounds = true }
        activity.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        val bitmap = activity.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        return bitmap?.let { rotated ->
            val oriented = rotateBitmap(rotated, orientation)
            Bitmap.createScaledBitmap(oriented, reqWidth, reqHeight, true)
        }
    }

    private fun calculateInSampleSize(options: Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == 0) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(orientation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}