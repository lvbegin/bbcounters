package com.app.bbcounters

import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.support.v7.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        println("Hello")
        val policy = ThreadPolicy.Builder().permitAll().build()

        StrictMode.setThreadPolicy(policy)
        System.setProperty(
            "java.protocol.handler.pkgs",
            "com.sun.net.ssl.internal.www.protocol"
        )
        try {
            val url = URL("https://data.mobility.brussels/bike/api/counts/")
            val con = url.openConnection()
            val read = BufferedReader(InputStreamReader(con.getInputStream()))
            var line = read.readLine()
            while (line != null) {
                println(line)
                line = read.readLine()
            }
        } catch (e: Exception) {
            println("An exception occured")
            e.printStackTrace()
        }

    }
}