package com.app.bbcounters

import android.support.test.runner.AndroidJUnit4
import android.util.Log

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataServerTest {
    @Test
    fun testGetDeviceList() {
        val dataServer = DataServer()
        val output = dataServer.getDevices()
        assert(output.isSuccess)
        val someExistingDeviceName = "CJM90"
        val outputValues = output.getOrNull()
        assert(outputValues != null)
        assert(1 == outputValues!!.filter { it.name == someExistingDeviceName }.count().toInt())
    }

    @Test
    fun testGetCountersValues() {
        val dataServer = DataServer()
        val counters = dataServer.getCurrentCounters()
        assert(counters.isSuccess)
        val someExistingDeviceName = "CJM90"
        val counterValues = counters.getOrNull()
        assert(counterValues != null)
        val deviceCountersArray = counterValues!!.filter { it.name == someExistingDeviceName }.toArray()
        assert(1 == deviceCountersArray.size)
        val deviceCounters = deviceCountersArray[0] as BikeCounterValue
        assert(deviceCounters.dayValue <= deviceCounters.yearValue)
    }

    @Test
    fun testGetCounterHistoryYear() {
        val dataServer = DataServer()
        val someExistingDeviceName = "CJM90"
        val yearHistory = dataServer.getCounterHistoryYear(someExistingDeviceName, "2023")
        assert(yearHistory.isSuccess)
        val yearHistoryValues = yearHistory.getOrNull()
        assert(yearHistoryValues != null)
        assert(yearHistoryValues!!.isNotEmpty())
    }
}