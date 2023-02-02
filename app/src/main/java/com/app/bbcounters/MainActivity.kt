package com.app.bbcounters

import android.content.Context
import android.os.Bundle
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import android.widget.Toast
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

class MainActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private var recyclerView : RecyclerView? = null
    private lateinit var detector : GestureDetectorCompat
    private var onTop = true
    private var toast: Toast? = null
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            showAppIcon()
            detector = GestureDetectorCompat(this,  this)
            recyclerView = findViewById<RecyclerView>(R.id.devicesList)
            val linearLayoutManager = LinearLayoutManager(this)
            recyclerView?.layoutManager = linearLayoutManager
            val dividerItemDecoration = DividerItemDecoration(
                recyclerView?.getContext(),
                linearLayoutManager.getOrientation()
            )
        recyclerView?.addItemDecoration(dividerItemDecoration)
        recyclerView?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean = detector.onTouchEvent(p1)
        })
        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onTop = (!recyclerView.canScrollVertically(-1) && dy <= 0)
            }
        })
        loadList()
    }

    private fun showAppIcon() {
        val actionBar = getSupportActionBar()
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setIcon(R.mipmap.ic_launcher_round)
        }
    }

    private fun loadList() {
        Executors.newSingleThreadExecutor().execute {
            val devicesDataArray = DisplayedCountersData.get()
            runOnUiThread {
                if (devicesDataArray.isEmpty())
                {
                    askIfRetry(this) { this@MainActivity.loadList() }
                }
                else {
                    recyclerView?.adapter = DeviceAdapter(devicesDataArray, this)
                    toast?.cancel()
                    toast = null
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (detector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onDown(p0: MotionEvent?): Boolean = false

    override fun onShowPress(p0: MotionEvent?) { }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean = false

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean = false

    override fun onLongPress(p0: MotionEvent?) { }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        if (p0 == null || p1 == null)
            return false
        return if (onTop && toast == null && p0.x - p1.x > 30) {
            toast = Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT)
            toast?.show()
            loadList()
            true
        } else
            false
    }
}