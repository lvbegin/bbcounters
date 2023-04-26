package com.app.bbcounters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream


class DisplayedCountersData private constructor(val name : String, val address : String,
                                                val dayValue : Int, val yearValue : Int,
                                                var picture : Bitmap?) : Parcelable {
        constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
            null,
    )  { }

    fun retrieveBitmap(context: Context)
    {
        val bytes = context.openFileInput("$name.jpg").readBytes()
        if (bytes.isNotEmpty())
            picture = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(address)
        parcel.writeInt(dayValue)
        parcel.writeInt(yearValue)
    }

    fun storeBitmapToFile(context: Context)
    {
        val stream = ByteArrayOutputStream()
        picture?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bytes: ByteArray = stream.toByteArray()
        val f : FileOutputStream = context.openFileOutput("$name.jpg", Context.MODE_PRIVATE);
        f.write(bytes)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<DisplayedCountersData> {
        override fun createFromParcel(parcel: Parcel) = DisplayedCountersData(parcel)

        override fun newArray(size: Int): Array<DisplayedCountersData?> = arrayOfNulls(size)

        fun get() : Array<DisplayedCountersData>  {
            val dataServer = DataServer()
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
                val pictureFromServer = dataServer.getPictures(device)
                val picture = pictureFromServer.getOrNull()
                if (pictureFromServer.isFailure)
                    Log.d("test output", "load picture from ${device.pictureURL} failed")
                else
                    Log.d("test output", "load picture from ${device.pictureURL} succeeded")
                val v = currentCountersArray.find { it.name == device.name }
                val day = v?.dayValue ?: -1
                val year = v?.yearValue ?: -1
                DisplayedCountersData(device.name, device.address, day, year, picture)
            }.toArray<DisplayedCountersData> { length -> arrayOfNulls<DisplayedCountersData>(length) }
        }
    }
}