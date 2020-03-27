package com.faciletech.heed.data

import android.content.Context
import com.faciletech.heed.utils.KeyConstants
import com.faciletech.heed.utils.SharedPreferenceManager
import com.mapbox.mapboxsdk.geometry.LatLng

private const val TAG = "LocationRepository"

class LocationRepository private constructor(
    private val sharedPreferenceManager: SharedPreferenceManager
) {

    /**
     * Returns all recorded locations from database.
     */
    fun getLocation(): LatLng? {
        val latitude = sharedPreferenceManager.getValueDouble(KeyConstants.LATITUDE)
        val longitude = sharedPreferenceManager.getValueDouble(KeyConstants.LONGITUDE)
        return if(latitude == null || longitude ==null) null else LatLng(latitude, longitude)
    }

    fun addLocation(latitude: Double, longitude: Double) {
        sharedPreferenceManager.save(KeyConstants.LATITUDE, latitude.toString())
        sharedPreferenceManager.save(KeyConstants.LONGITUDE, longitude.toString())
    }

    fun saveCurrentDistance(distanceInMeters: Double) {
        sharedPreferenceManager.save(KeyConstants.DISTANCE, distanceInMeters.toString())
    }
    fun getCurrentDistance(): Double? {
        return sharedPreferenceManager.getValueDouble(KeyConstants.DISTANCE)
    }

    companion object {
        @Volatile
        private var INSTANCE: LocationRepository? = null

        fun getInstance(context: Context): LocationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationRepository(SharedPreferenceManager(context))
                    .also { INSTANCE = it }
            }
        }
    }
}