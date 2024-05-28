package com.example.med6grp5supercykelstig

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

class DestinationTrackerService : Service() {

    private lateinit var locationManager: LocationManager
    private val proximityThreshold = 100 // Proximity threshold in meters
    private var startTime: Long = 0
    private var timerRunning = false
    private var distance = 0.0
    private var userID = ""
    private var targetLatitude = 0.0
    private var targetLongitude = 0.0
    private var stopThreshold = 0L
    private val NOTIFICATION_ID = 1233
    private val CHANNEL_ID = "DestinationTrackerChannel"

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetLatitude = intent?.getDoubleExtra("targetLatitude", 0.0) ?: 0.0
        targetLongitude = intent?.getDoubleExtra("targetLongitude", 0.0) ?: 0.0
        stopThreshold = intent?.getLongExtra("stopThreshold", 0L) ?: 0L
        userID = intent?.getStringExtra("userID") ?: ""

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        Log.d("TrackerServiceStart", "Tracking service started. lat: $targetLatitude, long: $targetLongitude, stopThresh: $stopThreshold, id: $userID")
        startTracking(targetLatitude, targetLongitude, stopThreshold, userID)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    private fun startTracking(targetLatitude: Double, targetLongitude: Double, stopThreshold: Long, userID: String) {
        startTime = System.currentTimeMillis()
        timerRunning = true
        startRouteLogs(userID)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            LOCATION_UPDATE_INTERVAL,
            0f,
            locationListener
        )

        saveTimeSpentTracking(0L)
    }

    private fun stopTracking() {
        locationManager.removeUpdates(locationListener)
        timerRunning = false
        Log.d("StopTracking", "StopTracking() called.")
        stopSelf()
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val distanceToDestination = calculateDistance(
                location.latitude,
                location.longitude,
                targetLatitude,
                targetLongitude
            )

            if (distanceToDestination <= proximityThreshold) {
                doSomething()
            }

            if (System.currentTimeMillis() - startTime >= stopThreshold * 1000) {
                stopTracking()
            }

        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun doSomething() {
        Log.d("doSomething() called", "doSomething() called in DestinationTrackerService")
        val timespent = System.currentTimeMillis() - startTime
        saveTimeSpentTracking(timespent)
        distance = readRouteDistance()
        insertRouteDistance()

        stopTracking()
    }

    private fun saveTimeSpentTracking(timeSpent: Long) {
        val fileName = "time_spent_to_destination.txt"
        val timeSpentSeconds = timeSpent / 1000
        val fileContent = "$timeSpentSeconds"
        val file = File(filesDir, fileName)
        FileOutputStream(file, false).bufferedWriter().use { writer ->
            writer.write(fileContent)
        }
    }

    private fun readRouteDistance(): Double {
        val fileName = "recent_route_distance.txt"
        val file = File(filesDir, fileName)
        var fileData = ""
        return if (file.exists() && file.length() > 0) {
            fileData = file.readText()
            fileData.toDouble()
        } else {
            0.0
        }
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
            dbHost,
            "-port",
            dbPort,
            "-database",
            dbName,
            "-username",
            dbUser,
            "-password",
            dbPassword,
            "-query",
            contributionString,
            userID,
            distance.toString()
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
            dbHost,
            "-port",
            dbPort,
            "-database",
            dbName,
            "-username",
            dbUser,
            "-password",
            dbPassword,
            "-query",
            logString,
            userID,
            currentDateTime
        )
    }

    private fun startRouteLogs(userID: String) {
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
            dbHost,
            "-port",
            dbPort,
            "-database",
            dbName,
            "-username",
            dbUser,
            "-password",
            dbPassword,
            "-query",
            logString,
            userID,
            currentDateTime
        )
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun buildNotification(): Notification {
        // Create a notification builder
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Destination Tracker")
            .setContentText("Tracking your destination...")
            .setSmallIcon(R.drawable.turtle_image)
        // Add any additional settings as needed

        // Build the notification
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Destination Tracker Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val LOCATION_UPDATE_INTERVAL = 10000L // Location update interval in milliseconds
    }
}