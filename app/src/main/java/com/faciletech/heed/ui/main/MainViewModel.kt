package com.faciletech.heed.ui.main

import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import com.faciletech.heed.data.LocationRepository
import com.mapbox.mapboxsdk.geometry.LatLng

class MainViewModel : ViewModel() {
    private lateinit var locationRepository: LocationRepository

    fun initialize(context: Context) {
        locationRepository = LocationRepository.getInstance(context)
    }

    fun updateCurrentLocation(latitude: Double, longitude: Double) {
        locationRepository.addLocation(latitude, longitude)
    }

    fun getLocation(): LatLng? {
        return locationRepository.getLocation()
    }

}
