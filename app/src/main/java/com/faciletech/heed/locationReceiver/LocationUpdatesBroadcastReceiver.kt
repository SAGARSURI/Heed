package com.faciletech.heed.locationReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.faciletech.heed.NotificationUtil
import com.faciletech.heed.data.LocationRepository
import com.google.android.gms.location.LocationResult
import com.mapbox.mapboxsdk.geometry.LatLng

class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {

    private val TAG = LocationUpdatesBroadcastReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() context:$context, intent:$intent")
        val locationRepository = LocationRepository.getInstance(context)
        val notificationUtil = NotificationUtil.getInstance(context)
        val savedLocation = locationRepository.getLocation()

        if (intent.action == ACTION_PROCESS_UPDATES) {
            LocationResult.extractResult(intent)?.let { locationResult ->
                locationResult.locations.map { location ->
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    val distanceInMeters = savedLocation?.distanceTo(currentLocation)
                    if (distanceInMeters != null) {
                        Log.e(TAG, "Distance in meters: $distanceInMeters")
                        if (distanceInMeters > 200f) {
                            //Notify user only once to carry sanitiser
                            if(locationRepository.getCurrentDistance() != null) {
                                if(locationRepository.getCurrentDistance()!! < 200f) {
                                    notificationUtil.buildNotification("Going somewhere!", "Carry mask and sanitizer. Tap to know more.", 1)
                                }
                            } else {
                                notificationUtil.buildNotification("Going somewhere!", "Carry mask and sanitizer. Tap to know more.", 1)
                            }
                        } else {
                            if(locationRepository.getCurrentDistance() != null) {
                                if(locationRepository.getCurrentDistance()!! > 200f) {
                                    notificationUtil.buildNotification("Back to home!", "Please wash your hands. Tap to know more.", 2)
                                }
                            } else {
                                notificationUtil.buildNotification("Back to home!", "Please wash your hands. Tap to know more.", 2)
                            }
                        }
                        locationRepository.saveCurrentDistance(distanceInMeters)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_PROCESS_UPDATES =
            "com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.action." +
                    "PROCESS_UPDATES"
    }
}