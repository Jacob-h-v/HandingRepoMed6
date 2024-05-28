package com.example.med6grp5supercykelstig

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.med6grp5supercykelstig.databinding.ActivityCommunityBinding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime
import kotlin.math.roundToInt


class CommunityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommunityBinding // Declare view binding variable

    private val dbHost = SharedValues.dbHost
    private val dbPort = SharedValues.dbPort
    private val dbName = SharedValues.dbName
    private val dbUser = SharedValues.dbUser
    private val dbPassword = SharedValues.dbPassword
    private var userID = ""
    private lateinit var distTravelTotal: TextView
    private var totalContributionString = ""
    private lateinit var scoreboardAdapter: ScoreboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get user ID from intent
        userID = intent.getStringExtra("USER_ID").toString()
        Log.d("User ID from Intent", "String value received from intent $userID")
        distTravelTotal = findViewById(R.id.distanceGoals)

        updateChart()

        // Set up the bottom navigation view
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_achievements_activity -> {
                    val intent = Intent(this, AchievementsActivity::class.java)
                    intent.putExtra("USER_ID", userID)
                    startActivity(intent);                    true
                }
                R.id.navigation_maps_activity-> {
                    val intent = Intent(this, MapsActivity::class.java)
                    intent.putExtra("USER_ID", userID)
                    startActivity(intent);                    true
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

        // Set the selected item in the bottom navigation view
        binding.bottomNavigation.menu.findItem(R.id.navigation_community_activity)?.isChecked = true

        //From Database needed: Distance traveled, distance left
        val kudosTextView = findViewById<TextView>(R.id.kudosid)
        val communityGoalsTextView = findViewById<TextView>(R.id.communityid)
        val socialTextView = findViewById<TextView>(R.id.socialid)

        kudosTextView.setOnClickListener {
            replaceFragment(KudosFragment())
        }

        communityGoalsTextView.setOnClickListener {
            replaceFragment(CommunityGoalsFragment())
        }

        socialTextView.setOnClickListener {
            replaceFragment(SocialFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null) // Optional: Add this transaction to the back stack
        transaction.commit()
    }




    //         Piechart logic
    private fun updateChart() {

        var goalAmount: Double
        var totalContributions = 0.0
        // Query for inserting contributions into the goal_contributions table
//        INSERT INTO goal_contributions (user_id, goal_id, user_contribution)
//        VALUES (user_id, 1, user_contribution)
//        ON DUPLICATE KEY UPDATE user_contribution = VALUES(user_contribution);


        // Construct the query string for insertion
        val goalAmountQuery = "SELECT goal_amount FROM goals WHERE id = 1"
        // Execute the query using DBQueryFacilitator AsyncTask
        val dbquery = DBQueryFacilitator({ success, resultValue ->
            if (success) {
                // Parse the result value into individual values
                goalAmount = resultValue.trim().toDouble()
                // Construct the query string for insertion
                val updateChartQuery = "SELECT SUM(user_contribution) FROM goal_contributions;"
                // Execute the query using DBQueryFacilitator AsyncTask
                val contributionQuery = DBQueryFacilitator({ boolean, value ->
                    if (boolean) {
                        // Parse the result value into individual values
                        if(value.trim() == "null")
                        {totalContributions = 0.0}
                        else{
                        totalContributions = value.trim().toDouble()
                        totalContributions = String.format("%.1f", totalContributions).toDouble()}

                        // Here values can be used as needed
                        // Update the text in the center of the chart:
                        totalContributionString = "$totalContributions / $goalAmount km"
                        distTravelTotal.text = totalContributionString

                        // Calculate the slice size and update the pie chart:
                        val pieChart = findViewById<ProgressBar>(R.id.goalProgressbar)
                        val d = totalContributions.toDouble() / goalAmount.toDouble()
                        val progress = (d * 100).toDouble()
                        Log.d("pieChart Progress", "pieChart Progress: $progress")
                        pieChart.progress = progress.roundToInt()

                        updateScoreboard()
                    }
                    else {
                        // Registration failed, display an error message
                        showToast(this, "Query execution failed.")
                    }
                    Log.d("totalContributions", "Total Contributions: $totalContributions")
                }, false)

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
                    updateChartQuery
                )
            } else {
                // Registration failed, display an error message
                showToast(this, "Query execution failed.")
            }
        }, false)




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
            goalAmountQuery
        )
    }

    private fun updateScoreboard() {
        val recyclerView: RecyclerView = findViewById(R.id.scoreboardRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Construct the query string using the retrieved text
        val scoreboardQueryString = "SELECT users.username, ROUND(SUM(goal_contributions.user_contribution), 1) AS total_contribution FROM defaultdb.users INNER JOIN defaultdb.goal_contributions ON users.id = goal_contributions.user_id GROUP BY users.username ORDER BY total_contribution DESC LIMIT 3;"

        Log.d("updateScoraboard String", "scoreboardQuery String: $scoreboardQueryString")

        // Execute the query using DBQueryFacilitator AsyncTask
        val scoreboardQuery = DBQueryFacilitator2({ bool, responseString ->
            if (bool) {
                // Split the response string by newline to get individual user data segments
                val userDataSegments = responseString.split("\n")

                // Process each user data segment
                val userList = mutableListOf<Pair<String, String>>()
                for (userDataSegment in userDataSegments) {
                    // Split the user data segment by comma to extract username and contributions
                    val userDataParts = userDataSegment.split(",")

                    // Ensure userDataParts contains at least two elements
                    if (userDataParts.size >= 2) {
                        // Extract username (first element)
                        val username = userDataParts[0]

                        // Extract contributions (remaining elements)
                        val contributions = userDataParts[1]

                        // Create a Pair containing username and contributions and add it to the list
                        userList.add(username to contributions)
                    }
                }

                // Set adapter data
                scoreboardAdapter = ScoreboardAdapter(userList)
                recyclerView.adapter = scoreboardAdapter
                updatePersonalContribution()
            } else {
                // Registration failed, display an error message
                updatePersonalContribution()
                showToast(this, "Something went wrong updating scoreboard")
            }
        }, false) // Pass true for executeUpdate (INSERT, UPDATE, DELETE), false for executeQuery (SELECT)

        scoreboardQuery.execute(
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
            scoreboardQueryString
        )
    }




    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun updatePersonalContribution()
    {
        // Construct the query string using the retrieved text
        val upQuery = "SELECT ROUND(SUM(user_contribution), 1) FROM defaultdb.goal_contributions WHERE user_id = ${userID};"

        // Update the queryString variable with the constructed query string
        Log.d("Login queryString", "Login Query String (pre-POST)$upQuery")



        // Execute the query using DBQueryFacilitator AsyncTask
        val personalQuery = DBQueryFacilitator({ result, contributionValue ->
            if (result) {
                if (contributionValue.trim() == "null" || contributionValue == "" || contributionValue == null)
                {
                    Log.d("contributionValue", "community page contributionValue: $contributionValue")
                    totalContributionString += "\n \n Your contribution\n in km: 0"
                    distTravelTotal.text = totalContributionString
                    updateCommunityLogs(userID)

                }
                else{
                Log.d("contributionValue", "community page contributionValue: $contributionValue")
                totalContributionString += "\n \n Your contribution\n in km: $contributionValue"
                distTravelTotal.text = totalContributionString
                updateCommunityLogs(userID)
                }
            } else {
                totalContributionString += "\n \n Your contribution\n in km: 0"
                distTravelTotal.text = totalContributionString
                updateCommunityLogs(userID)

            }
        }, false) // Pass true for executeUpdate (INSERT, UPDATE, DELETE), false for executeQuery (SELECT)

        personalQuery.execute(
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
            upQuery
        )
    }

    private fun updateCommunityLogs(userID: String)
    {
        val currentDateTime = LocalDateTime.now().toString()
        // Construct the query string using the retrieved text
        val query = "INSERT INTO activity_access_logs (user_id, activity_name, access_datetime) VALUES (${userID}, 'Community', '${currentDateTime}');"

        Log.d("Community LogString", "Community Log String (pre-POST)$query")

        // Execute the query using DBQueryFacilitator AsyncTask
        val logQuery = DBQueryFacilitator({ success, _ ->
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
            query
        )
    }
}


