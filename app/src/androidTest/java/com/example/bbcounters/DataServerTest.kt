package com.example.bbcounters

import android.support.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataServerTest {
    @Test
    fun testGetDeviceList() {
        val dataServer = DataServer()
        val output = dataServer.getDevices()
        val someExistingDeviceName = "CJM90"
        assert(1 == output.filter { it.name == someExistingDeviceName }.count().toInt())
    }

    @Test
    fun testGetCountersValues() {
        val dataServer = DataServer()
        val counters = dataServer.getCurrentCounters()
        val someExistingDeviceName = "CJM90"
        val deviceCountersArray = counters.filter { it.name == someExistingDeviceName }.toArray()
        assert(1 == deviceCountersArray.size)
        val deviceCounters = deviceCountersArray[0] as BikeCounterValue
        assert(deviceCounters.dayValue <= deviceCounters.yearValue)
    }
}