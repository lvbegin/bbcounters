package com.app.bbcounters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Room
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.Date

class DeviceStore(private val context : Context, private val dataServer : DataServer) {
    private val db = Room.databaseBuilder(context, DeviceDb::class.java, "device-db").build()
    private val timeToReloadImageInSec = 60 * 60 * 24 * 14;

    private fun pictureMustBeReloaded(deviceE : DeviceE?) = (deviceE == null) ||
            (deviceE.pictureFileName == null) ||
            ((deviceE.uploadTime + timeToReloadImageInSec) <  (Date().time / 1000))

    private fun filename(deviceName : String) = "$deviceName.jpg"

    fun updateImages(device : BikeCounterDevice) : Bitmap? {
        try {
            val deviceE = db.get().get(device.name)
            if (!pictureMustBeReloaded(deviceE))
                return null
            val pictureFromServer = dataServer.getPictures(device)
            val picture = pictureFromServer.getOrNull() ?: return null
            val filename = filename(device.name)
            val file = File(context.filesDir, filename)
            picture.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(file))
            val now = Date()
            val newDeviceE = DeviceE(device.name, filename, now.time / 1000)
            try {
                db.get().insert(newDeviceE)
            } catch (e: Exception) {
            }
            return picture
        } catch (e : Exception) {
            Log.d("test output", "${e.toString()}")
        }
        return null
    }

    fun get(device : BikeCounterDevice) : Bitmap? {
        var picture: Bitmap? = null
        val deviceE = db.get().get(device.name)
        if (!pictureMustBeReloaded(deviceE)) {
            Log.d("test output", "image stored")
            val filename = File(context.filesDir, filename(device.name)).toString()
            picture = BitmapFactory.decodeFile(filename)
        }
        return picture
    }
}