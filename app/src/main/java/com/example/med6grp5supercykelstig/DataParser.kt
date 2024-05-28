package com.example.med6grp5supercykelstig

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class DataParser {
    fun parse(jObject: JSONObject): List<List<HashMap<String, String>>> {
        val routes = mutableListOf<List<HashMap<String, String>>>()
        var jRoutes: JSONArray? = null
        try {
            jRoutes = jObject.getJSONArray("routes")
            for (i in 0 until jRoutes.length()) {
                val jLegs = jRoutes.getJSONObject(i).getJSONArray("legs")
                val path = mutableListOf<HashMap<String, String>>()
                for (j in 0 until jLegs.length()) {
                    val jSteps = jLegs.getJSONObject(j).getJSONArray("steps")
                    for (k in 0 until jSteps.length()) {
                        val polyline = jSteps.getJSONObject(k).getJSONObject("polyline").getString("points")
                        val list = decodePoly(polyline)
                        for (l in 0 until list.size) {
                            val hm = hashMapOf("lat" to list[l].latitude.toString(), "lng" to list[l].longitude.toString())
                            path.add(hm)
                        }
                    }
                    routes.add(path)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: Exception) {
            // Handle other exceptions
        }
        return routes

        // Inside DataParser class, after parsing the JSON

    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = (encoded[index++] - 63).code
                result = result or (b and 0x1F shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1)!= 0) -(result ushr 1) else result ushr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = (encoded[index++] - 63).code
                result = result or (b and 0x1F shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1)!= 0) -(result ushr 1) else result ushr 1
            lng += dlng

            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }

        return poly
        Log.d("DirectionsApi", "Parsed polyline points: $poly")

    }
}
