package com.app.bbcounters

import android.net.Uri
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

    private fun getConnectionOutputString(con : HttpsURLConnection) : Result<String> {
        try {
            con.doOutput = true;
            con.requestMethod = "GET";
            val input = BufferedReader(InputStreamReader(con.inputStream));
            var rc = "";
            if (con.responseMessage != "OK")
                return Result.failure(Exception("Get request failed"))
            input.forEachLine { rc += it; }
            return Result.success(rc)
        } finally {
            Log.v("test output", con.responseMessage)
            Log.v("test output", con.responseCode.toString())
        }
    }

    fun getDevices() : Result<Stream<BikeCounterDevice>> {
        return try {
            val connection = URL(uriDevices).openConnection() as HttpsURLConnection
            val responseAsString = getConnectionOutputString(connection)
            if (responseAsString.isFailure)
                return Result.failure(Exception("Cannot get devices information"))
            val response = responseAsString.getOrNull() ?: return Result.failure(Exception("Cannot get devices information"))
            val features = JSONObject(response).getJSONArray("features")
            Result.success(IntStream.range(0, features.length())
                    .mapToObj { it -> features.getJSONObject(it) }
                    .map { it ->
                        BikeCounterDevice(
                            it.getJSONObject("properties").getString("device_name"),
                            it.getJSONObject("properties").getString("road_fr")
                        )
                    })
        } catch (e : Exception) {
            Result.failure(Exception("Cannot get devices information"))
        }
    }

    fun getCurrentCounters() : Result<Stream<BikeCounterValue>> {
        return try {
            val connection = URL(uriCurrentCounters).openConnection() as HttpsURLConnection
            val responseAsString = getConnectionOutputString(connection)
            if (responseAsString.isFailure)
                return Result.failure(Exception("Cannot get current counters"))
            val response = responseAsString.getOrNull()
                ?: return Result.failure(Exception("Cannot get current counters"))
            val data = JSONObject(response).getJSONObject("data")
            return Result.success(StreamSupport
                .stream(
                    Spliterators.spliteratorUnknownSize(data.keys(), Spliterator.ORDERED),
                    false
                )
                .map {
                    val counters = data.getJSONObject(it)
                    BikeCounterValue(
                        it,
                        counters.getInt("day_cnt"),
                        counters.getInt("year_cnt")
                    )
                }.sorted { it1, it2 -> it1.name.compareTo(it2.name) })
        }
        catch (e : Exception) {
            Result.failure(Exception("Cannot get current counter"))
        }
      }

    fun getCounterHistoryYear(id : String, year: String) : Result<MutableMap<String, Int>> {
        return try {
            val firstDayYear = year + "0101"
            val lastDayYear = year + "1231"
            val uriCounterHistory = Uri.Builder()
                .scheme("https")
                .authority("data.mobility.brussels")
                .path("bike/api/counts/")
                .appendQueryParameter("request", "history")
                .appendQueryParameter("featureID", id)
                .appendQueryParameter("startDate", firstDayYear)
                .appendQueryParameter("endDate", lastDayYear)
                .build().toString();
            val connection = URL(uriCounterHistory).openConnection() as HttpsURLConnection;
            val responseAsString = getConnectionOutputString(connection);
            if (responseAsString.isFailure)
                return Result.failure(Exception("Cannot get yearly counter history"))
            val response = responseAsString.getOrNull()
                ?: return Result.failure(Exception("Cannot get yearly counter history"))
            val data = JSONObject(response).getJSONArray("data");
            var map = mutableMapOf<String, Int>()
            for (i in 0 until data.length()) {
                val entry = data.getJSONObject(i)
                val day = entry.get("count_date").toString()
                val v = entry.getInt("count")
                map.put(day, map.getOrDefault(day, 0) + v)
            }
            return Result.success(map)
        }
        catch (e : Exception) {
            Result.failure(Exception("Cannot get yearly counter history"))
        }
    }

}