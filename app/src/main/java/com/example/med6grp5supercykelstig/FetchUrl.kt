import android.content.Context
import android.os.AsyncTask
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class FetchURL(private val mContext: Context, private val onTaskCompleted: (Int, Double) -> Unit) : AsyncTask<String, Void, String>() {

    private var directionMode = "bicycling"

    // Change these to public instance variables
    var totalTime: Int = 0
    var totalDistance: Double = 0.0
    override fun doInBackground(vararg strings: String): String? {
        var data = ""
        directionMode = strings[1]
        return try {
            data = downloadUrl(strings[0])
            Log.d("mylog", "Background task data $data")
            data
        } catch (e: Exception) {
            Log.d("Background Task", e.toString())
            null
        }
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        Log.d("DirectionsApi", "Fetched JSON response: $result")

        // Parse the JSON response and calculate total distance and time
        calculateTotalDistanceAndTime(result?: "")

        // Call the onTaskCompleted function with the calculated values
        onTaskCompleted(totalTime, totalDistance)
    }

    private fun downloadUrl(strUrl: String): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        return try {
            val url = URL(strUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connect()
            iStream = urlConnection.inputStream
            val br = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuffer()
            var line = br.readLine()
            while (line!= null) {
                sb.append(line)
                line = br.readLine()
            }
            data = sb.toString()
            Log.d("mylog", "Downloaded URL: $data")
            br.close()
            data
        } catch (e: Exception) {
            Log.d("mylog", "Exception downloading URL: ${e.toString()}")
            ""
        } finally {
            iStream?.close()
            urlConnection?.disconnect()
        }
    }

    private fun calculateTotalDistanceAndTime(jsonResponse: String) {
        val jsonObject = JSONObject(jsonResponse)
        val routesArray = jsonObject.getJSONArray("routes")

        var totalDistance = 0.0
        var totalTime = 0

        for (i in 0 until routesArray.length()) {
            val routeObject = routesArray.getJSONObject(i)
            val legsArray = routeObject.getJSONArray("legs")

            for (j in 0 until legsArray.length()) {
                val legObject = legsArray.getJSONObject(j)
                val distanceObject = legObject.getJSONObject("distance")
                val durationObject = legObject.getJSONObject("duration")

                val distanceValue = distanceObject.getDouble("value")
                val durationValue = durationObject.getInt("value")

                val distanceInKm = distanceValue / 1000.0

                totalDistance += distanceInKm
                totalTime += durationValue


            }
            this.totalTime = totalTime
            this.totalDistance = totalDistance
            saveRouteDistance(this.totalDistance)
        }

        Log.d("Total Distance and Time", "Total Distance: $totalDistance km, Total Time: $totalTime seconds in FetchUrl.kt")
    }

    private fun saveRouteDistance(distance: Double) {
        val fileName = "recent_route_distance.txt"
        val fileContent = "$distance"
        Log.d("Saving Route Distance", "Saved Distance: $distance")
        val file = mContext.getFileStreamPath(fileName)
        file?.writeText(fileContent)
    }




//    companion object {
//        var totalTime: Int = 0
//        var totalDistance: Double = 0.0
//    }

}
