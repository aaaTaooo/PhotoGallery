package com.example.photogallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MINCOLS=1
const val MAXCOLS=3
class PhotoGalleryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoGalleryUiState())
    val uiState: StateFlow<PhotoGalleryUiState> = _uiState.asStateFlow()

    fun updateColumns(newCols: Int) {
        _uiState.update { currentState -> currentState.copy(columns = newCols) }
    }

    fun updatePermission(permission: Boolean) {
        _uiState.update { currentState -> currentState.copy(hasPermission = permission) }
    }

    fun loadPhotos(activity: MainActivity) {
        _uiState.update { currentState -> currentState.copy(isLoading = true) }

        //Load Photos in the IO Dispatchers
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = activity.contentResolver

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )

            val photos = mutableListOf<Photo>()
            val thumbnails = mutableMapOf<String, Bitmap>()

            val cursor = activity.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            cursor?.use { cursor ->
                val idColumns = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val orientationColumns = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idColumns)
                    val orientation = cursor.getInt(orientationColumns)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)

                    photos.add(Photo(id, orientation, width, height))

                    val inputStream = activity.contentResolver.openInputStream(
                        Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,id))
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if(bitmap != null) {
                        thumbnails[id] = bitmap
                    }
                }
            }
            _uiState.update {
                cursor -> cursor.copy(
                photos = photos,
                thumbnails = thumbnails,
                isLoading = false
                )
            }
        }
    }
}