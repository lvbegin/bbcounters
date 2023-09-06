package com.app.bbcounters

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceE(@PrimaryKey val id : String, val pictureFileName : String?, val uploadTime : Long)