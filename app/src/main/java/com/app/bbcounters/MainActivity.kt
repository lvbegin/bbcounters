package com.app.bbcounters

import android.content.Context
import android.os.Bundle
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.View.OnTouchListener
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors
import kotlin.math.abs


class DeviceAdapter(private val devices : Array<DisplayedCountersData>,
            private val context : Context, private val swipeDetector : OnTouchListener) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    class DeviceViewHolder(val view : View) : RecyclerView.ViewHolder(view) { }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_layout, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val idTextView = holder.view.findViewById<TextView>(R.id.deviceID)
        val addressTextView = holder.view.findViewById<TextView>(R.id.deviceAddress)
        val dailyCounterView = holder.view.findViewById<TextView>(R.id.daylyCounter)
        val yearlyCounterView = holder.view.findViewById<TextView>(R.id.yearlyCounter)
        val na = context.resources.getString(R.string.na)
        idTextView.text = devices[position].name
        addressTextView.text = devices[position].address
        dailyCounterView.text = if (devices[position].dayValue >=0) devices[position].dayValue.toString() else na
        yearlyCounterView.text = if (devices[position].yearValue >= 0) devices[position].yearValue.toString() else na
        holder.view.setOnClickListener { BikeCounterActivity.startActivity(context, devices[position].name) }
        holder.view.setOnTouchListener { v, e -> swipeDetector.onTouch(v, e) }
    }

    override fun getItemCount(): Int = devices.size
}

class DownSwipe() : OnTouchListener {
    private val swipeDetector = BasicSwipe()
    private var mustRegister = false
    var swipeIsRelevant : () -> Boolean = { false }
    @get:Deprecated(message="no getter available", level=DeprecationLevel.ERROR)
    var action: () -> Unit = { }
        set(value) { swipeDetector.action = value }
    @get:Deprecated(message="no getter available", level=DeprecationLevel.ERROR)
    var condition : (Pair<Float, Float>, Pair<Float, Float>) -> Boolean = {_, _ -> false }
        set(value) { swipeDetector.condition = value }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null)
            return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            this.mustRegister = this.swipeIsRelevant()
        }
       return if (this.mustRegister) swipeDetector.onTouch(v, event) else false
    }
}

class MainActivity : AppCompatActivity() {
    private val listDevicesParameter = "data"
    private var recyclerView : RecyclerView? = null
    private lateinit var detector : GestureDetectorCompat
    private var onTop = true
    private var toast: Toast? = null
    private var devicesDataArray : Array<DisplayedCountersData> ?= null
    private val swipeDetector = DownSwipe()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setIcon(this)
        swipeDetector.action = {
            toast = Toast.makeText(this, R.string.loading_message, Toast.LENGTH_SHORT)
            toast?.show()
            loadList()
        }
        swipeDetector.condition = { point1, point2 ->
            val deltaX = point1.first - point2.first
            val deltaY = point1.second - point2.second

            (deltaX < 100 && abs(deltaY) > 700)
        }
        swipeDetector.swipeIsRelevant = { this.onTop }

        recyclerView = findViewById<RecyclerView>(R.id.devicesList)
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView?.layoutManager = linearLayoutManager
        val dividerItemDecoration = DividerItemDecoration(
            recyclerView?.context,
            linearLayoutManager.orientation
        )
        recyclerView?.addItemDecoration(dividerItemDecoration)
        recyclerView?.setOnTouchListener { view, event -> swipeDetector.onTouch(view, event) }
        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onTop = (!recyclerView.canScrollVertically(-1) && dy <= 0)
            }
        })
        val savedData = savedInstanceState?.getParcelableArray(listDevicesParameter)
        if (savedData == null) {
            loadList()
        }
        else {
            val data : Array<DisplayedCountersData> = savedData as Array<DisplayedCountersData>
            devicesDataArray = data
            recyclerView?.adapter = DeviceAdapter(data, this, swipeDetector)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArray(listDevicesParameter, devicesDataArray)
    }

    private fun loadList() {
        Executors.newSingleThreadExecutor().execute {
            devicesDataArray = DisplayedCountersData.get()
            runOnUiThread {
                val isEmpty = devicesDataArray?.isEmpty() ?: return@runOnUiThread
                if (isEmpty)
                {
                    askIfRetry(this) { this@MainActivity.loadList() }
                }
                else {
                    val data = devicesDataArray ?: return@runOnUiThread
                    recyclerView?.adapter = DeviceAdapter(data, this, swipeDetector)
                    toast?.cancel()
                    toast = null
                }
            }
        }
    }
}
