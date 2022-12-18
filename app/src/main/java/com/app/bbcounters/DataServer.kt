package com.app.bbcounters

import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.util.*
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
}