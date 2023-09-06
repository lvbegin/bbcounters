package com.app.bbcounters

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DeviceE::class], version=1)
abstract class DeviceDb : RoomDatabase() {
    abstract fun get() : DeviceDAO
}