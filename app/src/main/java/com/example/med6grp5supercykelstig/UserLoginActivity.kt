package com.example.med6grp5supercykelstig

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.med6grp5supercykelstig.databinding.ActivityUserLoginBinding
import android.widget.EditText
import android.widget.Toast
import java.time.LocalDateTime

class UserLoginActivity : AppCompatActivity(){

    val dbHost = SharedValues.dbHost
    val dbPort = SharedValues.dbPort
    val dbName = SharedValues.dbName
    val dbUser = SharedValues.dbUser
    val dbPassword = SharedValues.dbPassword
    var queryString = ""
    lateinit var binding: ActivityUserLoginBinding // Declare binding as a class-level property
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var createAccountButton: Button
//    private val PERMISSION_REQUEST_CODE = 1001 // You can choose any integer value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserLoginBinding.inflate(layoutInflater) // Initialize binding
        setContentView(R.layout.activity_user_login)

        // Initialize EditText components
        usernameEditText = findViewById(R.id.username_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        loginButton = findViewById(R.id.login_button)
        createAccountButton = findViewById(R.id.create_account_button)


//        // Check if permissions are granted
//        if (!checkPermissions()) {
//            // Permissions are not granted, request them
//            requestPermissions()
//        }
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
//
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Notifications Disabled")
//            .setMessage("Notifications are currently disabled for this app. Enable notifications to receive important updates.")
//            .setPositiveButton("Open Settings") { dialog, _ ->
//                val intent = Intent()
//                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                val uri = Uri.fromParts("package", packageName, null)
//                intent.data = uri
//                startActivity(intent)
//                dialog.dismiss()
//            }
//            .setNegativeButton("Cancel") { dialog, _ ->
//                dialog.dismiss()
//            }
//            .show()



    }

    fun onLoginClick(view: View) {
        // Get the text from the EditText components
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        val hashedPassword = PasswordHasher.hashPasswordWithSalt(password)

    // Construct the query string using the retrieved text
    val query = "SELECT id FROM users WHERE username = '$username' AND password_hash = '$hashedPassword'"

    // Update the queryString variable with the constructed query string
    queryString = query
    Log.d("Login queryString", "Login Query String (pre-POST)"+queryString)



        // Execute the query using DBQueryFacilitator AsyncTask
        val dbquery = DBQueryFacilitator({ success, resultValue ->
            if (success) {
                updateActivityLogs(resultValue)
                // Registration successful, display toast message
                showToast(this, "Login Successful.")
                val intent = Intent(this, MapsActivity::class.java)
                intent.putExtra("USER_ID", resultValue)
                startActivity(intent)

            } else {
                // Registration failed, display an error message
                showToast(this, "Login Failed.")
            }
        }, false) // Pass true for executeUpdate (INSERT, UPDATE, DELETE), false for executeQuery (SELECT)

        dbquery.execute(
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
            queryString
        )
    }

    private fun showToast(ctx: Context, message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun onCreateAccountClick(view: View) {
        val fragment = CreateAccountFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)  // Optional: add transaction to back stack
            .commit()

        // Disable buttons
        loginButton.isEnabled = false
        createAccountButton.isEnabled = false

    }

    fun onFragmentDismissed() {
        // Enable buttons
        loginButton.isEnabled = true
        createAccountButton.isEnabled = true
    }

    fun updateActivityLogs(userID: String)
    {
        val currentDateTime = LocalDateTime.now().toString()
        // Construct the query string using the retrieved text
        val query = "INSERT INTO activity_access_logs (user_id, activity_name, access_datetime) VALUES (${userID}, 'logged_in', '${currentDateTime}');"

        // Update the queryString variable with the constructed query string
        var logString = query
        Log.d("Login logString", "logging string (pre-POST)"+logString)



        // Execute the query using DBQueryFacilitator AsyncTask
        val logQuery = DBQueryFacilitator({ success, resultValue ->
            if (success) {
                // Registration successful, display toast message
                showToast(this, "Login Successful.")
                val intent = Intent(this, MapsActivity::class.java)
                intent.putExtra("USER_ID", userID)
                startActivity(intent)

            } else {
                // Registration failed, display an error message
                showToast(this, "Login Failed.")
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

//    private fun checkPermissions(): Boolean {
//        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//        val foregroundServicePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
//
//        return (coarseLocationPermission == PackageManager.PERMISSION_GRANTED &&
//                fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
//                foregroundServicePermission == PackageManager.PERMISSION_GRANTED)
//    }

//    private fun requestPermissions() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.FOREGROUND_SERVICE
//            ),
//            PERMISSION_REQUEST_CODE
//        )
//    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == PERMISSION_REQUEST_CODE) {
//            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//                // Permissions granted, proceed with your task
//            } else {
//                // Permissions not granted, handle accordingly (e.g., show a message to the user)
//            }
//        }
//    }


}

