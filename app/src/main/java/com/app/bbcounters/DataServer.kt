package com.app.bbcounters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
        .build().toString()

    private val uriCurrentCounters = Uri.Builder()
        .scheme("https")
        .authority("data.mobility.brussels")
        .path("bike/api/counts/")
        .appendQueryParameter("request", "live")
        .build().toString()

    private fun getConnectionOutputString(con : HttpsURLConnection) : Result<String> {
        try {
            con.doOutput = true
            con.requestMethod = "GET"
            val input = BufferedReader(InputStreamReader(con.inputStream))
            var rc = ""
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
                    .mapToObj { features.getJSONObject(it) }
                    .map { BikeCounterDevice(
                            it.getJSONObject("properties").getString("device_name"),
                            it.getJSONObject("properties").getString("road_fr"),
                            it.getJSONObject("properties").getString("picture_1")
                        )
                    })
        } catch (e : Exception) {
            Result.failure(Exception("Cannot get devices information"))
        }
    }

    fun getPictures(device : BikeCounterDevice) : Result<Bitmap> {
            return try {
                val connection = URL(device.pictureURL).openConnection() as HttpsURLConnection
                Result.success(BitmapFactory.decodeStream(connection.inputStream))
            } catch (e : Exception) {
                Result.failure(Exception("Cannot retrieve picture"))
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
                        counters.getInt("hour_cnt"),
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
                .build().toString()
            val connection = URL(uriCounterHistory).openConnection() as HttpsURLConnection
            val responseAsString = getConnectionOutputString(connection)
            if (responseAsString.isFailure)
                return Result.failure(Exception("Cannot get yearly counter history"))
            val response = responseAsString.getOrNull()
                ?: return Result.failure(Exception("Cannot get yearly counter history"))
            val data = JSONObject(response).getJSONArray("data")
            val map = mutableMapOf<String, Int>()
             (0 until data.length()).forEach() {
                val entry = data.getJSONObject(it)
                val day = entry.get("count_date").toString()
                val v = entry.getInt("count")
                map[day] = map.getOrDefault(day, 0) + v
            }
            return Result.success(map)
        }
        catch (e : Exception) {
            Result.failure(Exception("Cannot get yearly counter history"))
        }
    }

}