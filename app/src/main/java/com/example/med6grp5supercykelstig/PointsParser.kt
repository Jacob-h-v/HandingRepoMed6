


import android.content.Context
import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import com.example.med6grp5supercykelstig.DataParser
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject

/**
 * Created by Vishal on 10/20/2018.
 */

class PointsParser(private val mContext: Context, private val directionMode: String) :
    AsyncTask<String, Integer, List<List<HashMap<String, String>>>>() {

    private var taskCallback: TaskLoadedCallback? = null

    init {
        taskCallback = mContext as TaskLoadedCallback
    }

    // Parsing the data in non-ui thread
    override fun doInBackground(vararg jsonData: String): List<List<HashMap<String, String>>>? {
        var jObject: JSONObject? = null
        var routes: List<List<HashMap<String, String>>>? = null

        return try {
            jObject = JSONObject(jsonData[0])
            Log.d("mylog", jsonData[0])
            val parser = DataParser()
            Log.d("mylog", parser.toString())

            // Starts parsing data
            routes = parser.parse(jObject)
            Log.d("mylog", "Executing routes")
            Log.d("mylog", routes.toString())

            routes
        } catch (e: Exception) {
            Log.d("mylog", e.toString())
            e.printStackTrace()
            // Return an empty list instead of null
            emptyList()
        }
    }

    // Executes in UI thread, after the parsing process
    override fun onPostExecute(result: List<List<HashMap<String, String>>>) {
        val points: ArrayList<LatLng> = ArrayList()
        var lineOptions: PolylineOptions? = null
        // Traversing through all the routes
        for (i in 0 until result.size) {
            points.clear()
            lineOptions = PolylineOptions()
            // Fetching i-th route
            val path = result[i]
            // Fetching all the points in i-th route
            for (j in 0 until path.size) {
                val point = path[j]
                val lat = point["lat"]!!.toDouble()
                val lng = point["lng"]!!.toDouble()
                val position = LatLng(lat, lng)
                points.add(position)
            }
            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points)
            if (directionMode.toLowerCase() == "walking") {
                lineOptions.width(10f).color(Color.MAGENTA)
            } else {
                lineOptions.width(20f).color(Color.BLUE)
                lineOptions.width(20f).color(Color.BLUE)
            }
            Log.d("mylog", "onPostExecute lineoptions decoded")
        }

        // Drawing polyline in the Google Map for the i-th route
        if (lineOptions!= null) {
            //mMap.addPolyline(lineOptions)
            taskCallback?.onTaskDone(lineOptions)
        } else {
            Log.d("mylog", "without Polylines drawn")
        }
    }
}
