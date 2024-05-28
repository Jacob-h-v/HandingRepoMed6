package com.example.med6grp5supercykelstig

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import android.widget.TextView
import androidx.core.view.GravityCompat
import com.example.med6grp5supercykelstig.databinding.ActivityAchievementsBinding
import java.io.File
import java.time.LocalDateTime
import kotlin.math.round


class AchievementsActivity : AppCompatActivity() {

    private val TAG = "YourActivityName"
    private var userID = ""
    private var timeFile = ""
    private var receivedDistance: Double = 0.0

    private lateinit var binding: ActivityAchievementsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG,"Starting personal")
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get user ID from intent
        userID = intent.getStringExtra("USER_ID").toString()
        Log.d("User ID from Intent", "String value received from intent "+userID)

        updatePersonalLogs(userID)
        timeFile = readTimeSpentTracking()
        receivedDistance = readRouteDistance()

        // Set up the bottom navigation view
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_community_activity -> {
                    val intent = Intent(this, CommunityActivity::class.java)
                    intent.putExtra("USER_ID", userID)
                    startActivity(intent)
                    true
                }

                R.id.navigation_maps_activity -> {
                    Log.d(TAG, "Navigating home")
                    val intent = Intent(this, MapsActivity::class.java)
                    intent.putExtra("USER_ID", userID)
                    startActivity(intent)
                    true
                }


                R.id.navigation_achievements_activity -> {
                    val intent = Intent(this, AchievementsActivity::class.java)
                    intent.putExtra("USER_ID", userID)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Set the selected item in the bottom navigation view
        binding.bottomNavigation.menu.findItem(R.id.navigation_achievements_activity)?.isChecked = true


        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        binding.openDrawerButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }
        binding.buttonInDrawer.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }


        val distance = receivedDistance.toFloat()
        val roundedDistance = String.format("%.1f", distance).toFloat()
        Log.d("roundedDistance","Rounded distance: $roundedDistance")
        val seconds = timeFile.toFloat()

        //Database switching text
        val speedTextView = findViewById<TextView>(R.id.speedid)
        val yourSpeedText = findViewById<TextView>(R.id.yourSpeedText)
        val durationTextView = findViewById<TextView>(R.id.durationid)
        val distanceTextView = findViewById<TextView>(R.id.distanceid)

        if (timeFile != "0" && distance > 0.1)
        {
            saveLastRouteDistance(roundedDistance)
            saveLastRouteTime(seconds)
        }
        else{

        }


        val lastTime = readLastTime()
        val lastDist = readLastDistance()
        if (lastTime > 0.1f && lastDist > 0.1f) {
            val avgSpeed = lastDist / (lastTime / 3600f)
            val roundToOneDecimal = String.format("%.2f", avgSpeed).toFloat()
            val speed = roundToOneDecimal

            speedTextView.text = "Average speed: " + roundToOneDecimal + "km/h"
            durationTextView.text = "Duration: " + lastTime + "seconds"
            distanceTextView.text = "Distance: " + lastDist + "km"


            val imageView = findViewById<ImageView>(R.id.imageView)

//        speedTextView.text = speed.toString()
//        durationTextView.text = duration.toString()
//        distanceTextView.text = distance.toString()

//        Animal speed image resources
            val slowAnimal = R.drawable.turtle_image
            val mediumSpeedAnimal = R.drawable.elephant_image
            val fastAnimal = R.drawable.komodo_image
            val veryFastAnimal = R.drawable.wolf_image


            // Define constants for animal speeds in km/h
            val slowLimit = 5f
            val mediumLimit = 15f
            val fastLimit = 25f
            // Animal speed images cases
            when {
                speed > 0.1f && speed <= slowLimit -> imageView.setImageResource(slowAnimal)
                speed > slowLimit && speed <= mediumLimit -> imageView.setImageResource(
                    mediumSpeedAnimal
                )

                speed > mediumLimit && speed <= fastLimit -> imageView.setImageResource(fastAnimal)
                speed > fastLimit -> imageView.setImageResource(veryFastAnimal)
                else -> {
                    // Handle unexpected speed values
                    imageView.setImageResource(R.drawable.turtle_image)
                }
            }
        }
        else{

        }





//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
    }

    fun updatePersonalLogs(userID: String)
    {
        val currentDateTime = LocalDateTime.now().toString()
        // Construct the query string using the retrieved text
        val query = "INSERT INTO activity_access_logs (user_id, activity_name, access_datetime) VALUES (${userID}, 'Personal', '${currentDateTime}');"

        // Update the queryString variable with the constructed query string
        var logString = query
        Log.d("Community LogString", "Community Log String (pre-POST)"+logString)



        // Execute the query using DBQueryFacilitator AsyncTask
        val logQuery = DBQueryFacilitator({ success, resultValue ->
            if (success) {

            } else {
            }
        }, true) // Pass true for executeUpdate (INSERT, UPDATE, DELETE), false for executeQuery (SELECT)

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
            logString
        )
    }

    private fun readTimeSpentTracking(): String {
        val fileName = "time_spent_to_destination.txt"
        val file = File(this.filesDir, fileName)
        return try {
            if (file.exists() && file.length() > 0) {
                file.readText()
            } else {
                "0" // Return a default value if the file doesn't exist or is empty
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading time spent tracking", e)
            "0" // Return a default value in case of an error
        }
    }

    private fun readRouteDistance(): Double {
        val fileName = "last_finished_distance.txt"
        val file = File(this.filesDir, fileName)

        return try {
            if (file.exists() && file.length() > 0) {
                val fileData = file.readText()
                Log.d("fileData Distance", "Distance Being Saved: $fileData")
                fileData.toDoubleOrNull() ?: 0.0 // Use toDoubleOrNull() to handle conversion errors
            } else {
                0.0
            }
        } catch (e: NumberFormatException) {
            Log.e("Read Distance Error", "Error converting distance to double: ${e.message}")
            0.0 // Return a default value in case of conversion error
        }
    }

    private fun saveLastRouteTime(time: Float) {
        val fileName = "last_finished_route_time.txt"
        val fileContent = "$time"
        Log.d("Saving Route Distance", "Saved Distance: $time")
        val file = this.getFileStreamPath(fileName)
        file?.writeText(fileContent)
    }

    private fun saveLastRouteDistance(dist: Float) {
        val fileName = "last_finished_route_distance.txt"
        val fileContent = "$dist"
        Log.d("Saving Route Distance", "Saved Distance: $dist")
        val file = this.getFileStreamPath(fileName)
        file?.writeText(fileContent)
    }

    private fun readLastDistance(): Float {
        val fileName = "last_finished_route_distance.txt"
        val file = File(this.filesDir, fileName)

        return try {
            if (file.exists() && file.length() > 0) {
                val fileData = file.readText()
                Log.d("fileData Distance", "Distance Being Saved: $fileData")
                fileData.toFloatOrNull() ?: 0f // Use toDoubleOrNull() to handle conversion errors
            } else {
                0f
            }
        } catch (e: NumberFormatException) {
            Log.e("Read Distance Error", "Error converting distance to double: ${e.message}")
            0f // Return a default value in case of conversion error
        }
    }

    private fun readLastTime(): Float {
        val fileName = "last_finished_route_time.txt"
        val file = File(this.filesDir, fileName)

        return try {
            if (file.exists() && file.length() > 0) {
                val fileData = file.readText()
                Log.d("fileData Distance", "Distance Being Saved: $fileData")
                fileData.toFloatOrNull() ?: 0f // Use toDoubleOrNull() to handle conversion errors
            } else {
                0f
            }
        } catch (e: NumberFormatException) {
            Log.e("Read Distance Error", "Error converting distance to double: ${e.message}")
            0f // Return a default value in case of conversion error
        }
    }
}