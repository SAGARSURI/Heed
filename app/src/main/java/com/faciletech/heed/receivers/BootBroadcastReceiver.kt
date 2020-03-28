package com.faciletech.heed.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.faciletech.heed.data.LocationRepository
import com.faciletech.heed.data.MyLocationManager

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
                Log.e(BootBroadcastReceiver::class.java.simpleName, "boot receiver")
                val locationManager = MyLocationManager.getInstance(context)
                val locationRepository = LocationRepository.getInstance(context)
                locationRepository.getLocation()?.let {
                    locationManager.startLocationUpdates()
                }
            }
        }
    }

}