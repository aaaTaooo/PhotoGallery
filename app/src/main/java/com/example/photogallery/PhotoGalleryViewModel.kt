package com.example.photogallery

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

}