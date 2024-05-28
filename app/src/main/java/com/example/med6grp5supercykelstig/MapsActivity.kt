package com.example.med6grp5supercykelstig

import FetchURL
import PlaceAutocompleteAdapter
import TaskLoadedCallback
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.med6grp5supercykelstig.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.data.kml.KmlLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, TaskLoadedCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var sessionToken: AutocompleteSessionToken
    private lateinit var binding: ActivityMapsBinding
    private lateinit var recyclerView: RecyclerView
    private val userMarkers = mutableListOf<Marker>()
    var userID = ""
    val context = this


    private var routeTime: Int = 0
    private var totalDistance: Double = 0.0
    private var targetLatitude = 0.0
    private var targetLongitude = 0.0
    private var distance = 0.0;

//    private val place1 = LatLng(57.01782, 9.99309)
//    private val place2 = LatLng(57.04841, 9.92982)


    private var isSubmittingQuery = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.autocompleteSuggestions.visibility = View.GONE


//        binding.mapNavigationButton.setOnClickListener {
//            clearMapPolylines()
//        }
        userID = intent.getStringExtra("USER_ID").toString()
        Log.d("Maps Received Intent", "User_ID: "+userID)

        val mapNavigationButton = findViewById<Button>(R.id.map_navigation_button)
        mapNavigationButton.setOnClickListener {
            readRouteDistance()
            readRouteStartTime()
            showToast(this, "Route Saved")

        }


        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, getMapsApiKey())
            Log.d("MapsActivity", "Places API initialized successfully.")
        }

        this@MapsActivity.recyclerView = binding.autocompleteSuggestions
        recyclerView.layoutManager = LinearLayoutManager(this)
        Log.d("MapsActivity", "RecyclerView initialized.")

        placesClient = Places.createClient(this)
        sessionToken = AutocompleteSessionToken.newInstance()
        Log.d("MapsActivity", "PlacesClient and SessionToken initialized.")

        val autocompleteAdapter = PlaceAutocompleteAdapter(this, placesClient, sessionToken)
        recyclerView.adapter = autocompleteAdapter
        Log.d("MapsActivity", "PlaceAutocompleteAdapter instantiated.")

        binding.searchBubble.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && !isSubmittingQuery) {
                    isSubmittingQuery = true
//                    // Initialize DestinationTracker with the target latitude and longitude
//                    destinationTracker = DestinationTracker(context, targetLatitude, targetLongitude, totalTime)
//
//                    // Call a method in DestinationTracker to start location tracking
//                    destinationTracker.startTracking()
                    // Pass a lambda function as the callback to getUserCurrentLocation
                    getUserCurrentLocation(this@MapsActivity) { userLocation ->
                        // Convert the query to LatLng
                        val destinationLatLng = convertAddressToLatLng(this@MapsActivity, query)
                        targetLatitude = destinationLatLng?.latitude ?: 0.0
                        targetLongitude = destinationLatLng?.longitude ?: 0.0
                        if (destinationLatLng != null) {
                            // Use the destinationLatLng here

                            fetchDirectionsJson(userLocation, query, "bicycling", LatLng(57.036560, 9.949020))

                            launchGoogleMapsWithWaypointAndThenDestination(
                                userLocation,
                                LatLng(57.036560, 9.949020), // This is the waypoint
                                destinationLatLng // This is the destination


                            )



                        } else {
                            Toast.makeText(this@MapsActivity, "Unable to geocode address.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    binding.autocompleteSuggestions.visibility = View.GONE
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null && newText.isNotEmpty()) {
                    fetchSuggestions(newText) // Fetch suggestions based on the new text
                    // Update your RecyclerView here
                    binding.autocompleteSuggestions.visibility = View.VISIBLE

                }
                else{
                    binding.autocompleteSuggestions.visibility = View.GONE

                }
                if (isSubmittingQuery) {
                    isSubmittingQuery = false
                }
                return true
            }
        })




        binding.searchBubble.setOnCloseListener {
            // Hide the RecyclerView when SearchView is closed
            binding.autocompleteSuggestions.visibility = View.GONE
            false
        }

        requestLocationPermission { granted ->
            if (granted) {
                getUserCurrentLocation(this) { userLocation ->
                    if (userLocation != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12.0f))
                    } else {
                        Toast.makeText(this, "Unable to retrieve location.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Location permission is required to provide directions.", Toast.LENGTH_SHORT).show()
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_achievements_activity -> {
                    val intent = Intent(this, AchievementsActivity::class.java)
                    intent.putExtra("USER_ID", userID)
                    startActivity(intent)
                    true
                }
                R.id.navigation_maps_activity -> {
                    val intent = Intent(this, MapsActivity::class.java)
                    intent.putExtra("USER_ID", userID)
                    startActivity(intent)
                    true
                }
                R.id.navigation_community_activity -> {
                    val intent = Intent(this, CommunityActivity::class.java)
                    intent.putExtra("USER_ID", userID)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.menu.findItem(R.id.navigation_maps_activity)?.isChecked = true


    }


//    private fun clearMapPolylines() {
//        mMap.clear() // This clears all markers, polylines, and other elements on the map
//         If you specifically want to clear only polylines, you can iterate through the map's polylines and remove them
//         mMap.polylines.forEach { polyline -> polyline.remove() }
//        val kmlLayer = KmlLayer(mMap, R.raw.supercykelstig, this)
//        kmlLayer.addLayerToMap()
//    }


    private fun fetchSuggestions(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val predictions = findFilteredPredictions(query)
            withContext(Dispatchers.Main) {
                (recyclerView.adapter as PlaceAutocompleteAdapter).updateDataList(predictions)
            }
        }
    }

    private suspend fun findFilteredPredictions(query: String): List<AutocompletePrediction> {
        return suspendCoroutine { continuation ->
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(sessionToken)
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    continuation.resume(response.autocompletePredictions)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val aalborg = LatLng(57.048, 9.921)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(aalborg, 12.0f))

        val kmlLayer = KmlLayer(mMap, R.raw.supercykelstig, this)
        kmlLayer.addLayerToMap()


    }

    private fun getMapsApiKey(): String {
        return "MAPS_API_KEY"
    }

    override fun onTaskDone(vararg values: Any?) {
        if (values.isNotEmpty() && values[0] is PolylineOptions) {
            val polylineOptions = values[0] as PolylineOptions
        }
    }

    private fun createCustomMarker(context: Context): BitmapDescriptor? {
        try {
            // Retrieve the drawable resource
            val drawable = ContextCompat.getDrawable(context, R.drawable.circle_marker)
            if (drawable == null) {
                Log.e("MapsActivity", "Error: Drawable resource not found.")
                return null
            }

            // Create a bitmap from the drawable
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            // Resize the bitmap to a smaller size
            val resizedWidth = 50 // Desired width
            val resizedHeight = 50 // Desired height
            val resizedBitmap = Bitmap.createBitmap(resizedWidth, resizedHeight, Bitmap.Config.ARGB_8888)
            val resizedCanvas = Canvas(resizedBitmap)
            resizedCanvas.scale(resizedWidth.toFloat() / bitmap.width, resizedHeight.toFloat() / bitmap.height)
            resizedCanvas.drawBitmap(bitmap, 0f, 0f, null)

            // Return the BitmapDescriptor from the resized bitmap
            return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
        } catch (e: Exception) {
            // Log the exception
            Log.e("MapsActivity", "Error creating custom marker: ${e.message}", e)
            // Optionally, handle the error further here
            return null
        }
    }



    private fun getUserCurrentLocation(context: Context, callback: (LatLng?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, handle accordingly
            callback(null)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location!= null) {
                val userLocation = LatLng(location.latitude, location.longitude)
                callback(userLocation)

                // Attempt to find an existing marker for the user's location
                val existingMarker = userMarkers.firstOrNull { it.position == userLocation }

                if (existingMarker!= null) {
                    // Marker exists, update its position and ensure it's visible
                    existingMarker.position = userLocation
                    existingMarker.isVisible = true
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12.0f))
                    Log.d("MapsActivity", "Updated User location marker: $userLocation")
                } else {
                    // No existing marker, create a new one
                    val newMarker = mMap.addMarker(MarkerOptions().icon(createCustomMarker(context)).position(userLocation).title("User Location"))
                    if (newMarker!= null) {
                        userMarkers.add(newMarker)
                    }
                    Log.d("MapsActivity", "Created new User location marker: $userLocation")
                }
            } else {
                // Handle the case where the location is null
                callback(null)
            }
        }
    }







    private fun convertAddressToLatLng(context: Context, address: String): LatLng? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocationName(address, 1)
            if (addresses!= null && addresses.isNotEmpty()) {
                val latitude = addresses[0].latitude
                val longitude = addresses[0].longitude
                LatLng(latitude, longitude)
            } else {
                null
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", "Error converting address to LatLng: ${e.message}")
            null
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }


    private fun requestLocationPermission(callback: (Boolean) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Permission is already granted, proceed with fetching the user's location
            callback(true)
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted, proceed with fetching the user's location
                    getUserCurrentLocation(this) { userLocation ->
                        if (userLocation!= null) {
                            // Permission was granted, and the user's location is available
                            // Now you can proceed with using the user's location
                            // For example, move the camera to the user's location
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12.0f))
                        } else {
                            // Handle the case where the location is null
                            Toast.makeText(this, "Unable to retrieve location.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Permission was denied, inform the user
                    Toast.makeText(this, "Location permission is required to provide directions.", Toast.LENGTH_SHORT).show()
                }
                // Call the superclass's implementation of onRequestPermissionsResult
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }


    private fun fetchDirectionsJson(origin: LatLng?, destination: String, directionMode: String, waypoint: LatLng?) {
        val apiKey = SharedValues.googleAPIKey
        val originStr = origin?.let { "origin=${it.latitude},${it.longitude}" }?: ""
        val destStr = "destination=$destination"
        val modeStr = "mode=$directionMode"
        val waypointStr = waypoint?.let { "waypoints=optimize:true|${it.latitude},${it.longitude}" }?: ""

        val url = "https://maps.googleapis.com/maps/api/directions/json?$originStr&$destStr&$waypointStr&$modeStr&key=$apiKey"

        Log.d("DirectionsApi", "Fetching directions URL: $url")
        FetchURL(this) { totalTime, totalDistance ->
            routeTime = totalTime
            this.totalDistance = totalDistance
            Log.d("Total Distance and Time", "Total Distance: $totalDistance km, Total Time: $routeTime seconds (in MapsActivity)")

            saveRouteStartTime()
            // Initialize DestinationTracker with the target latitude and longitude
//            val serviceIntent = Intent(this@MapsActivity, DestinationTrackerService::class.java)
//            serviceIntent.putExtra("targetLatitude", targetLatitude)
//            serviceIntent.putExtra("targetLongitude", targetLongitude)
//            serviceIntent.putExtra("stopThreshold", 1500L)
//            serviceIntent.putExtra("userID", userID)
//            Log.d("AttemptingServiceStart", "lat: $targetLatitude, long: $targetLongitude, thresh: ${routeTime.toLong()}, id: $userID")
//            this@MapsActivity.startService(serviceIntent)
        }.execute(url, directionMode)
    }


    private fun launchGoogleMapsWithWaypointAndThenDestination(userLocation: LatLng?, waypoint: LatLng, finalDestination: LatLng) {
        // Combine the waypoints and destinations into a single intent
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${userLocation?.latitude},${userLocation?.longitude}&waypoints=${waypoint.latitude},${waypoint.longitude}&destination=${finalDestination.latitude},${finalDestination.longitude}"))
        intent.setPackage("com.google.android.apps.maps")

        // Check if the intent can be handled by Google Maps
        if (intent.resolveActivity(packageManager)!= null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Google Maps is not installed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRouteStartTime() {
        val fileName = "route_start_time.txt"
        val startTime = System.currentTimeMillis()
        val fileContent = "$startTime"
        val file = File(filesDir, fileName)
        FileOutputStream(file, false).bufferedWriter().use { writer ->
            writer.write(fileContent)
        }
        startRouteLogs()
    }

    private fun readRouteStartTime() {
        val fileName = "route_start_time.txt"
        val file = File(filesDir, fileName)
        var startTime = 0L
        // Check if the file exists
        if (!file.exists() || !file.isFile) {
            return
        }
        // Read the contents of the file
        val fileData = file.readText()
        if(fileData == ""){return}
        startTime = fileData.trim().toLong()


        val timeSpent = (System.currentTimeMillis() - startTime) / 1000

        saveTimeSpent(timeSpent)

        resetRouteStartTime()
    }

    private fun saveTimeSpent(timeSpent: Long) {
        val fileName = "time_spent_to_destination.txt"
        val timeSpentSeconds = timeSpent
        val fileContent = "$timeSpentSeconds"
        val file = File(this.filesDir, fileName)
        FileOutputStream(file, false).bufferedWriter().use { writer ->
            writer.write(fileContent)
        }
    }

    private fun resetRouteStartTime() {
        val fileName = "route_start_time.txt"
        val fileContent = ""
        val file = File(filesDir, fileName)
        FileOutputStream(file, false).bufferedWriter().use { writer ->
            writer.write(fileContent)
        }
    }

    private fun readRouteDistance() {
        val fileName = "recent_route_distance.txt"
        val file = File(filesDir, fileName)
        var fileData = ""
        if (file.exists() && file.length() > 0) {
            fileData = file.readText()
            distance = fileData.trim().toDouble()
            insertRouteDistance()
        } else {
            insertRouteDistance()
            distance = 0.0
        }
    }

    private fun startRouteLogs() {
        val currentDateTime = LocalDateTime.now().toString()
        val query = "INSERT INTO activity_access_logs (user_id, activity_name, access_datetime) VALUES ($userID, 'Route Started', '$currentDateTime');"

        var logString = query

        val logQuery = DBQueryFacilitator({ success, _ ->
            if (success) {

            } else {
            }
        }, true)

        logQuery.execute(
            "-host",
            SharedValues.dbHost,
            "-port",
            SharedValues.dbPort,
            "-database",
            SharedValues.dbName,
            "-username",
            SharedValues.dbUser,
            "-password",
            SharedValues.dbPassword,
            "-query",
            logString,
            userID,
            currentDateTime
        )
    }

    private fun updateRouteLogs() {
        val currentDateTime = LocalDateTime.now().toString()
        val query = "INSERT INTO activity_access_logs (user_id, activity_name, access_datetime) VALUES ($userID, 'Route Finished. Distance: $distance', '$currentDateTime');"

        var logString = query

        val logQuery = DBQueryFacilitator({ success, _ ->
            if (success) {


            } else {

            }
        }, true)

        logQuery.execute(
            "-host",
            SharedValues.dbHost,
            "-port",
            SharedValues.dbPort,
            "-database",
            SharedValues.dbName,
            "-username",
            SharedValues.dbUser,
            "-password",
            SharedValues.dbPassword,
            "-query",
            logString
        )
        saveRouteDistance(distance)
        distance = 0.0
        resetRouteDistance(distance)
    }

    private fun insertRouteDistance() {
        val query = "INSERT INTO defaultdb.goal_contributions (user_id, goal_id, user_contribution) VALUES ($userID, 1, $distance);"

        var contributionString = query

        val contributionQuery = DBQueryFacilitator({ success, resultValue ->
            if (success) {
                updateRouteLogs()
            } else {
                updateRouteLogs()
            }
        }, true)

        contributionQuery.execute(
            "-host",
            SharedValues.dbHost,
            "-port",
            SharedValues.dbPort,
            "-database",
            SharedValues.dbName,
            "-username",
            SharedValues.dbUser,
            "-password",
            SharedValues.dbPassword,
            dbPassword,
            "-query",
            contributionString
        )
    }

    private fun resetRouteDistance(distance: Double) {
        val fileName = "recent_route_distance.txt"
        val fileContent = "$distance"
        Log.d("Saving Route Distance", "Saved Distance: $distance")
        val file = this.getFileStreamPath(fileName)
        file?.writeText(fileContent)
    }

    private fun saveRouteDistance(distance: Double) {
        val fileName = "last_finished_distance.txt"
        val fileContent = "$distance"
        Log.d("Saving Route Distance", "Saved Distance: $distance")
        val file = this.getFileStreamPath(fileName)
        file?.writeText(fileContent)
    }

    private fun showToast(ctx: Context, message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}