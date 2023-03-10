package com.app.bbcounters

import android.os.Parcel
import android.os.Parcelable
import java.util.stream.Stream

class DisplayedCountersData private constructor(val name : String, val address : String,
                                                val dayValue : Int, val yearValue : Int) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt()
    )  { }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(address)
        parcel.writeInt(dayValue)
        parcel.writeInt(yearValue)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DisplayedCountersData> {
        override fun createFromParcel(parcel: Parcel): DisplayedCountersData {
            return DisplayedCountersData(parcel)
        }

        override fun newArray(size: Int): Array<DisplayedCountersData?> = arrayOfNulls(size)


        fun get() : Array<DisplayedCountersData>  {
            val dataServer = DataServer()
            val devices : Stream<BikeCounterDevice>? = dataServer.getDevices()?.sorted { it, it2 -> it.name.compareTo(it2.name) }
            val currentCounters = dataServer.getCurrentCounters().sorted { it, it2 -> it.name.compareTo(it2.name) }
            val currentCountersArray = currentCounters.toArray<BikeCounterValue> { length -> arrayOfNulls<BikeCounterValue>(length) }

            return devices?.map { device ->
                val v = currentCountersArray.find { it.name == device.name }
                val day = if (v != null) v.dayValue else -1
                val year = if (v != null) v.yearValue else -1
                DisplayedCountersData(device.name, device.address, day, year)
            }?.toArray<DisplayedCountersData> { length -> arrayOfNulls<DisplayedCountersData>(length) }
                ?: arrayOf<DisplayedCountersData>()
        }
    }
}