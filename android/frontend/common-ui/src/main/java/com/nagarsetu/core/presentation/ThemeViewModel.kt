package com.nagarsetu.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.core.data.local.PreferencesManager
import com.nagarsetu.core.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferences: PreferencesManager
) : ViewModel() {
    val theme = preferences.appTheme.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppTheme.CIVIC_LIGHT
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { preferences.setAppTheme(theme) }
    }
}
