package com.app.bbcounters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeviceDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(deviceE: DeviceE) : Long

    @Query("Select * from devices where id = :id")
    fun get(id : String) : DeviceE?
}