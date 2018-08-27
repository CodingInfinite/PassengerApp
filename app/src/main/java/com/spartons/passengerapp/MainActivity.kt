package com.spartons.passengerapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.FirebaseDatabase
import com.spartons.passengerapp.collection.MarkerCollection
import com.spartons.passengerapp.helpers.FirebaseEventListenerHelper
import com.spartons.passengerapp.helpers.GoogleMapHelper
import com.spartons.passengerapp.helpers.MarkerAnimationHelper
import com.spartons.passengerapp.helpers.UiHelper
import com.spartons.passengerapp.interfaces.FirebaseDriverListener
import com.spartons.passengerapp.interfaces.IPositiveNegativeListener
import com.spartons.passengerapp.interfaces.LatLngInterpolator
import com.spartons.passengerapp.model.Driver
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), FirebaseDriverListener {

    companion object {
        private const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 6161
        private const val ONLINE_DRIVERS = "online_drivers"
    }

    private lateinit var googleMap: GoogleMap
    private lateinit var locationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var locationFlag = true
    private lateinit var valueEventListener: FirebaseEventListenerHelper
    private val uiHelper = UiHelper()
    private val googleMapHelper = GoogleMapHelper()
    private val databaseReference = FirebaseDatabase.getInstance().reference.child(ONLINE_DRIVERS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.supportMap) as SupportMapFragment
        mapFragment.getMapAsync { googleMap = it }
        createLocationCallback()
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = uiHelper.getLocationRequest()
        if (!uiHelper.isPlayServicesAvailable(this)) {
            Toast.makeText(this, "Play Services did not installed!", Toast.LENGTH_SHORT).show()
            finish()
        } else requestLocationUpdate()
        valueEventListener = FirebaseEventListenerHelper(this)
        databaseReference.addChildEventListener(valueEventListener)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdate() {
        if (!uiHelper.isHaveLocationPermission(this)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
            return
        }
        if (uiHelper.isLocationProviderEnabled(this))
            uiHelper.showPositiveDialogWithListener(this, resources.getString(R.string.need_location), resources.getString(R.string.location_content), object : IPositiveNegativeListener {
                override fun onPositive() {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }, "Turn On", false)
        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                if (locationResult!!.lastLocation == null) return
                val latLng = LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                Log.e("Location", latLng.latitude.toString() + " , " + latLng.longitude)
                if (locationFlag) {
                    locationFlag = false
                    animateCamera(latLng)
                }
            }
        }
    }

    private fun animateCamera(latLng: LatLng) {
        val cameraUpdate = googleMapHelper.buildCameraUpdate(latLng)
        googleMap.animateCamera(cameraUpdate, 10, null)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            val value = grantResults[0]
            if (value == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Location Permission denied", Toast.LENGTH_SHORT).show()
                finish()
            } else if (value == PackageManager.PERMISSION_GRANTED) requestLocationUpdate()
        }
    }

    override fun onDriverAdded(driver: Driver) {
        val markerOptions = googleMapHelper.getDriverMarkerOptions(LatLng(driver.lat, driver.lng))
        val marker = googleMap.addMarker(markerOptions)
        marker.tag = driver.driverId
        MarkerCollection.insertMarker(marker)
        totalOnlineDrivers.text = resources.getString(R.string.total_online_drivers).plus(" ").plus(MarkerCollection.allMarkers().size)
    }

    override fun onDriverRemoved(driver: Driver) {
        MarkerCollection.removeMarker(driver.driverId)
        totalOnlineDrivers.text = resources.getString(R.string.total_online_drivers).plus(" ").plus(MarkerCollection.allMarkers().size)
    }

    override fun onDriverUpdated(driver: Driver) {
        val marker = MarkerCollection.getMarker(driverId = driver.driverId)
        MarkerAnimationHelper.animateMarkerToGB(marker!!, LatLng(driver.lat, driver.lng), LatLngInterpolator.Spherical())
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseReference.removeEventListener(valueEventListener)
        locationProviderClient.removeLocationUpdates(locationCallback)
        MarkerCollection.clearMarkers()
    }
}
