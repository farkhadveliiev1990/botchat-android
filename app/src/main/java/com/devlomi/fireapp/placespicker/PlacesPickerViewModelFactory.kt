package com.devlomi.fireapp.placespicker

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devlomi.fireapp.activities.placespicker.PlacesPickerViewModel

class PlacesPickerViewModelFactory(private val context: Context, private val lifecycleOwner: LifecycleOwner) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        return PlacesPickerViewModel(context, lifecycleOwner) as T
    }
}