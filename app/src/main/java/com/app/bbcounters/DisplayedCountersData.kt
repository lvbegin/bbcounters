package com.app.bbcounters

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import kotlin.contracts.contract


class DisplayedCountersData private constructor(val name : String, val address : String,
                                                val hourValue : Int,
                                                val dayValue : Int, val yearValue : Int,
                                                val pictureURL : String?,
                                                var picture : Bitmap?) : Parcelable {
        constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString(),
        null,
    )

    fun retrieveBitmap(context: Context)
    {
        val bytes = context.openFileInput("$name.jpg").readBytes()
        if (bytes.isNotEmpty())
            picture = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(address)
        parcel.writeInt(hourValue)
        parcel.writeInt(dayValue)
        parcel.writeInt(yearValue)
        parcel.writeString(pictureURL)
    }

    fun storeBitmapToFile(context: Context)
    {
        val stream = ByteArrayOutputStream()
        picture?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bytes: ByteArray = stream.toByteArray()
        context.openFileOutput("$name.jpg", Context.MODE_PRIVATE).write(bytes)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<DisplayedCountersData> {

        override fun createFromParcel(parcel: Parcel) = DisplayedCountersData(parcel)

        override fun newArray(size: Int): Array<DisplayedCountersData?> = arrayOfNulls(size)

        fun get(deviceStore : DeviceStore, dataServer : DataServer) : Array<DisplayedCountersData>  {
            val devicesFromServer = dataServer.getDevices()
            if (devicesFromServer.isFailure)
                return emptyArray()
            val devices = dataServer.getDevices().getOrNull()?.sorted { it, it2 -> it.name.compareTo(it2.name) }
                ?: return emptyArray()
            val currentCountersFromServer = dataServer.getCurrentCounters()
            if (currentCountersFromServer.isFailure)
                return emptyArray()
            val currentCounters = currentCountersFromServer.getOrNull()?.sorted { it, it2 -> it.name.compareTo(it2.name) }
                ?: return emptyArray()
            val currentCountersArray = currentCounters.toArray<BikeCounterValue> { length -> arrayOfNulls<BikeCounterValue>(length) }

            return devices.map { device ->
                val picture = deviceStore.get(device)
                val v = currentCountersArray.find { it.name == device.name }
                val hour = v?.hourValue ?: -1
                val day = v?.dayValue ?: -1
                val year = v?.yearValue ?: -1
                DisplayedCountersData(device.name, device.address, hour, day, year, device.pictureURL, picture)
            }.toArray { length -> arrayOfNulls<DisplayedCountersData>(length) }
        }
    }
}