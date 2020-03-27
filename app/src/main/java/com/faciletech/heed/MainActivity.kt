package com.faciletech.heed

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModelProvider
import com.faciletech.heed.data.MyLocationManager
import com.faciletech.heed.ui.main.MainViewModel
import com.faciletech.heed.utils.KeyConstants
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Feature.*
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.android.synthetic.main.main_activity.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var locationManager: MyLocationManager
    private lateinit var viewModel: MainViewModel

    private var droppedMarkerLayer: Layer? = null
    private val DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID"
    private val MARKER_SOURCE = "markers-source";
    private val MARKER_STYLE_LAYER = "markers-style-layer";
    private val MARKER_IMAGE = "custom-marker";
    private val mLocationRequest: LocationRequest by lazy { LocationRequest() }
    private val TAG = MainActivity::class.java.simpleName
    private lateinit var mapBox: MapboxMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
    private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    private val LOCATION_PERMISSION_INDEX = 0
    private val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

    private var mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            if (locationResult != null) {
                val location = locationResult.locations.first {
                    it != null
                }
                val currentLocation = LatLng(location!!.latitude, location.longitude)
                animateCameraToPosition(currentLocation)
                fusedLocationClient.removeLocationUpdates(this)
            } else {
                Log.w(TAG, "getLastLocation:exception")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            getString(R.string.access_token)
        )
        setContentView(R.layout.main_activity)
        viewModel = ViewModelProvider(this)
            .get(MainViewModel::class.java)
        viewModel.initialize(this)

        mapView?.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = MyLocationManager.getInstance(this)
        mapView?.getMapAsync(this)
        intent?.let {
            showTipDialog(it.getIntExtra(KeyConstants.TIP_TYPE, -1))
        }
        updateButtonText()
    }

    private fun updateButtonText() {
        viewModel.getLocation()?.let {
            setLocation.text = getString(R.string.label_change_home_location)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            showTipDialog(it.getIntExtra(KeyConstants.TIP_TYPE, -1))
        }
        updateButtonText()
    }

    private fun showTipDialog(tipType: Int) {
        if (tipType != -1) {
            val bottomSheetDialogFragment = TipsFragment()
            val bundle = Bundle()
            bundle.putInt(KeyConstants.TIP_TYPE, tipType)
            bottomSheetDialogFragment.arguments = bundle
            bottomSheetDialogFragment.show(supportFragmentManager, bottomSheetDialogFragment.tag)
        }
    }

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    enableLocationDialog(exception)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setMessage("Please select \"All the time\" as the option. This will allow the app to notify you when you are leaving or entering the home.")
                dialogBuilder.setPositiveButton(
                    "Ok"
                ) { _, _ ->
                    checkDeviceLocationSettingsAndStartGeofence()
                }
                dialogBuilder.create().show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                getLastLocation()
            }
        }
    }

    private fun enableLocationDialog(exception: ResolvableApiException) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Please enable the location in your device.")
        dialogBuilder.setPositiveButton(
            "Ok"
        ) { _, _ ->
            exception.startResolutionForResult(
                this@MainActivity,
                REQUEST_TURN_DEVICE_LOCATION_ON
            )
        }
        dialogBuilder.show()
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            this@MainActivity,
            permissionsArray,
            resultCode
        )
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapBox = mapboxMap
        mapBox.setStyle(Style.MAPBOX_STREETS) { style ->
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments.
            val marker = ImageView(this@MainActivity)
            marker.setImageResource(R.drawable.custom_marker)
            val params = FrameLayout.LayoutParams(
                120,
                120,
                Gravity.CENTER
            )
            marker.layoutParams = params
            mapView.addView(marker)

            initDroppedMarker(style)
            viewModel.getLocation()?.let {
                setLocationOnMap(marker, style, it)
            } ?: changeLocationOnMap(marker, style)

            setLocation.setOnClickListener {
                if (marker.visibility == View.VISIBLE) {
                    val getCameraPosition = mapBox.cameraPosition.target
                    setLocationOnMap(marker, style, getCameraPosition)
                } else {
                    changeLocationOnMap(marker, style)
                }
            }
        }
        checkPermissionsAndStartGeofencing()
    }

    private fun changeLocationOnMap(
        marker: ImageView,
        style: Style
    ) {
        locationManager.stopLocationUpdates()
        marker.visibility = View.VISIBLE
        droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID)
        if (droppedMarkerLayer != null) {
            droppedMarkerLayer?.setProperties(visibility(NONE))
        }
        setLocation.text = getString(R.string.label_set_home_location)
    }

    private fun setLocationOnMap(
        marker: ImageView,
        style: Style,
        location: LatLng
    ) {
        locationManager.startLocationUpdates()
        viewModel.updateCurrentLocation(location.latitude, location.longitude)
        marker.visibility = View.INVISIBLE
        if (style.getLayer(DROPPED_MARKER_LAYER_ID) != null) {
            style.getSourceAs<GeoJsonSource>("dropped-marker-source-id")?.setGeoJson(
                Point.fromLngLat(
                    location.longitude,
                    location.latitude
                )
            )
            droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID)
            if (droppedMarkerLayer != null) {
                droppedMarkerLayer?.setProperties(visibility(VISIBLE))
            }
        }
        setLocation.text = "Change Location"
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (viewModel.getLocation() != null) {
            animateCameraToPosition(viewModel.getLocation()!!)
        } else {
            mLocationRequest.interval = TimeUnit.SECONDS.toMillis(60) // two minute interval
            mLocationRequest.fastestInterval = TimeUnit.SECONDS.toMillis(30)
            mLocationRequest.maxWaitTime = TimeUnit.MINUTES.toMillis(2)
            mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            fusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.myLooper()
            )
        }
    }

    private fun animateCameraToPosition(currentLocation: LatLng) {
        mapBox.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15.0))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            // Permission denied.
            // Displays App settings screen.
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setMessage("Please select \"All the time\" as the option. This will allow the app to notify you when you are leaving or entering the home.")
            dialogBuilder.setPositiveButton(
                "Ok"
            ) { dialog, _ ->
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            dialogBuilder.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun addMarkers(
        loadedMapStyle: Style,
        currentLocation: LatLng
    ) {
        val features: MutableList<Feature> = ArrayList()
        features.add(
            fromGeometry(
                Point.fromLngLat(
                    currentLocation.longitude,
                    currentLocation.latitude
                )
            )
        )

/* Source: A data source specifies the geographic coordinate where the image marker gets placed. */

        /* Source: A data source specifies the geographic coordinate where the image marker gets placed. */
        loadedMapStyle.addSource(
            GeoJsonSource(MARKER_SOURCE, FeatureCollection.fromFeatures(features))
        )

/* Style layer: A style layer ties together the source and image and specifies how they are displayed on the map. */
        /* Style layer: A style layer ties together the source and image and specifies how they are displayed on the map. */
        loadedMapStyle.addLayer(
            SymbolLayer(MARKER_STYLE_LAYER, MARKER_SOURCE)
                .withProperties(
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconImage(MARKER_IMAGE),
                    PropertyFactory.iconSize(0.3f),// Adjust the second number of the Float array based on the height of your marker image.
// This is because the bottom of the marker should be anchored to the coordinate point, rather
// than the middle of the marker being the anchor point on the map.
                    PropertyFactory.iconOffset(arrayOf(0f, -52f))
                )
        )
    }

    private fun Style.applyMarker(
        context: Context,
        markerTag: String,
        marker: Int,
        currentLocation: LatLng
    ) {
        val bitmap = AppCompatResources.getDrawable(context, marker)
            ?.toBitmap()
        addImage(markerTag, bitmap!!)
        addMarkers(this, currentLocation)
    }

    private fun initDroppedMarker(loadedMapStyle: Style) {
        // Add the marker image to map
        loadedMapStyle.addImage(
            "dropped-icon-image", BitmapFactory.decodeResource(
                resources, R.drawable.custom_marker
            )
        );
        loadedMapStyle.addSource(GeoJsonSource("dropped-marker-source-id"));
        loadedMapStyle.addLayer(
            SymbolLayer(
                DROPPED_MARKER_LAYER_ID,
                "dropped-marker-source-id"
            ).withProperties(
                iconImage("dropped-icon-image"),
                visibility(NONE),
                iconAllowOverlap(true),
                iconSize(0.3f),
                iconIgnorePlacement(true)
            )
        )
    }

}

private fun Location.toLatLng(): LatLng {
    return LatLng(this.latitude, this.longitude)
}
