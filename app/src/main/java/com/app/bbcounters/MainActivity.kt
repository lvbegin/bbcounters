package com.app.bbcounters

import android.content.Context
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.concurrent.Executors


class DeviceAdapter(private val devices : Array<DisplayedCountersData>,
            private val context : Context) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    class DeviceViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        var deviceId : String? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_layout, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val idTextView = holder.view.findViewById<TextView>(R.id.deviceID)
        val AddressTextView = holder.view.findViewById<TextView>(R.id.deviceAddress)
        val daylyCounterView = holder.view.findViewById<TextView>(R.id.daylyCounter)
        val yearlyCounterView = holder.view.findViewById<TextView>(R.id.yearlyCounter)
        idTextView.text = devices[position].name
        AddressTextView.text = devices[position].address
        daylyCounterView.text = if (devices[position].dayValue >=0) devices[position].dayValue.toString() else "n/a"
        yearlyCounterView.text = if (devices[position].yearValue >= 0) devices[position].yearValue.toString() else "n/a"
        holder.view.setOnClickListener { BikeCounterActivity.startActivity(context, devices[position].name) }
    }

    override fun getItemCount(): Int = devices.size
}

class MainActivity : AppCompatActivity() {
    var recyclerView : RecyclerView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            val actionBar = getSupportActionBar()
            if (actionBar != null) {
                actionBar.setDisplayShowHomeEnabled(true)
                actionBar.setIcon(R.mipmap.ic_launcher_round)
            }
            recyclerView = findViewById<RecyclerView>(R.id.devicesList)
            val linearLayoutManager = LinearLayoutManager(this)
            recyclerView?.layoutManager = linearLayoutManager
        val dividerItemDecoration = DividerItemDecoration(
            recyclerView?.getContext(),
            linearLayoutManager.getOrientation()
        )
        recyclerView?.addItemDecoration(dividerItemDecoration)
            Executors.newSingleThreadExecutor().execute {
                val devicesDataArray = DisplayedCountersData.get()
                runOnUiThread {
                    recyclerView?.adapter = DeviceAdapter(devicesDataArray, this)
                }
            }
    }
}
