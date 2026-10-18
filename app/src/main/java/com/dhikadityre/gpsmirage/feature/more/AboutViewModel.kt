package com.dhikadityre.gpsmirage.feature.more

import androidx.lifecycle.ViewModel
import com.dhikadityre.dev.tools.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AboutUiState(
    val appName: String = "GPS Mirage",
    val versionName: String = BuildConfig.VERSION_NAME,
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val developerName: String = "Dhikadityre",
    val developerRole: String = "Android Software Engineer",
    val appPurpose: String = "Aplikasi pengujian geolokasi dan simulasi fake GPS berbasis radius acuan (geofence) untuk riset, evaluasi batas wilayah terkontrol, serta perlindungan privasi.",
    val targetSdk: Int = 35,
    val minSdk: Int = 23
)

@HiltViewModel
class AboutViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()
}
