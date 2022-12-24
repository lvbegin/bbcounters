package com.app.bbcounters

import android.net.Uri
import android.util.Log
import android.util.Range
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale
import java.util.stream.IntStream
import java.util.stream.Stream
import java.util.stream.StreamSupport
import javax.net.ssl.HttpsURLConnection


class DataServer {
    private val uriDevices = Uri.Builder()
        .scheme("https")
        .authority("data.mobility.brussels")
        .path("bike/api/counts/")
        .appendQueryParameter("request", "devices")
        .build().toString();

    private val uriCurrentCounters = Uri.Builder()
        .scheme("https")
        .authority("data.mobility.brussels")
        .path("bike/api/counts/")
        .appendQueryParameter("request", "live")
        .build().toString();

    private fun getConnectionOutputString(con : HttpsURLConnection) : String {
        try {
            con.doOutput = true;
            con.requestMethod = "GET";
            val input = BufferedReader(InputStreamReader(con.inputStream));
            var rc = "";
            input.forEachLine { rc += it; };
            return rc;
        } finally {
            Log.v("test output", con.responseMessage)
            Log.v("test output", con.responseCode.toString())
        }
    }

    fun getDevices() : Stream<BikeCounterDevice> {
        val connection = URL(uriDevices).openConnection() as HttpsURLConnection;
        val responseAsString = getConnectionOutputString(connection);
        val features = JSONObject(responseAsString).getJSONArray("features");
        return IntStream.range(0, features.length())
            .mapToObj {it -> features.getJSONObject(it) }
            .map { it -> BikeCounterDevice(
                it.getJSONObject("properties").getString("device_name"),
                it.getJSONObject("properties").getString("road_fr"))}
    }

    fun getCurrentCounters() : Stream<BikeCounterValue> {
        val connection = URL(uriCurrentCounters).openConnection() as HttpsURLConnection;
        val responseAsString = getConnectionOutputString(connection);
        val data = JSONObject(responseAsString).getJSONObject("data");
        return StreamSupport
            .stream(Spliterators.spliteratorUnknownSize(data.keys(),Spliterator.ORDERED), false)
            .map {
                val counters = data.getJSONObject(it)
                BikeCounterValue(
                    it,
                    counters.getInt("day_cnt"),
                    counters.getInt("year_cnt"))
            }.sorted { it1, it2 -> it1.name.compareTo(it2.name) }
      }

    fun getCounterHistory(id : String) : MutableMap<String, Int> {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val currentDate = sdf.format(Date())
        val uriCounterHistory = Uri.Builder()
            .scheme("https")
            .authority("data.mobility.brussels")
            .path("bike/api/counts/")
            .appendQueryParameter("request", "history")
            .appendQueryParameter("featureID", id)
            .appendQueryParameter("startDate", "20181205")
            .appendQueryParameter("endDate", currentDate)
            .build().toString();
        val connection = URL(uriCounterHistory).openConnection() as HttpsURLConnection;
        val responseAsString = getConnectionOutputString(connection);
        val data = JSONObject(responseAsString).getJSONArray("data");
        var map = mutableMapOf<String, Int>()
        for (i in 0 until data.length()) {
            val entry = data.getJSONObject(i)
            val year = entry.get("count_date").toString().slice(IntRange(0, 3))
            val v = entry.getInt("count")
            map.put(year, map.getOrDefault(year, 0) + v)
        }
        return map
    }
}