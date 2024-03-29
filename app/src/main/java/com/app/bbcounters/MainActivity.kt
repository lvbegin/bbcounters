package com.app.bbcounters

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.util.Arrays
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.math.abs


class DeviceAdapter(private val devices : Array<DisplayedCountersData>,
            private val context : Activity, private val swipeDetector : OnTouchListener) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    class DeviceViewHolder(val view : View) : RecyclerView.ViewHolder(view)

    var clickX : Int = 0
    var clickY : Int = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_layout, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val addressTextView = holder.view.findViewById<TextView>(R.id.deviceAddress)
        val hourCounterView = holder.view.findViewById<TextView>(R.id.hourCounter)
        val dailyCounterView = holder.view.findViewById<TextView>(R.id.dailyCounter)
        val yearlyCounterView = holder.view.findViewById<TextView>(R.id.yearlyCounter)
        val imageView = holder.view.findViewById<ImageView>(R.id.image_device)
        val na = context.resources.getString(R.string.na)
        addressTextView.text = devices[position].address
        hourCounterView?.text = if (devices[position].hourValue >=0) devices[position].hourValue.toString() else na
        dailyCounterView.text = if (devices[position].dayValue >=0) devices[position].dayValue.toString() else na
        yearlyCounterView.text = if (devices[position].yearValue >= 0) devices[position].yearValue.toString() else na
        val pictureAvailable = devices[position].picture != null
        if (pictureAvailable)
            imageView.setImageBitmap(devices[position].picture)
        else
            imageView.setImageResource(R.drawable.ic_launcher_foreground)
        imageView.setOnClickListener { it ->
            if (!pictureAvailable)
                return@setOnClickListener
            val coordinates = IntArray(2)
            it.getLocationOnScreen(coordinates)
            val devicesWIthPicture = Arrays.stream(devices).filter {device -> device.picture != null }
                                        .toArray { size -> arrayOfNulls<DisplayedCountersData>(size) }
                                        as Array<DisplayedCountersData>
            val positionWithPicture = devicesWIthPicture.indexOfFirst {
                device -> device.name == devices[position].name
            }
            PictureActivity.startActivity(context, devicesWIthPicture, positionWithPicture, coordinates[0].toFloat(), coordinates[1].toFloat())
        }
        
        holder.view.setOnClickListener {
            BikeCounterActivity.startActivity(context, devices[position].name)
        }
        holder.view.setOnLongClickListener {
            val x = it.x.toInt() + this@DeviceAdapter.clickX
            val y = it.y.toInt() + this@DeviceAdapter.clickY
            graphShortcutCallback(context, devices, position, x, y, Gravity.TOP + Gravity.LEFT)
        }
        holder.view.setOnTouchListener { v, e ->
            if (e?.action == MotionEvent.ACTION_DOWN)
            {
                clickX = e?.x?.toInt() ?: 0
                clickY = e?.y?.toInt() ?: 0
            }
            swipeDetector.onTouch(v, e)
        }
    }

    override fun getItemCount(): Int = devices.size
}

class DownSwipe : OnTouchListener {
    private val swipeDetector = BasicSwipe()
    private var mustRegister = false
    var swipeIsRelevant : () -> Boolean = { false }
    @get:Deprecated(message="no getter available", level=DeprecationLevel.ERROR)
    var action: () -> Unit
        set(value) { swipeDetector.action = value }
        get() = { }

    @get:Deprecated(message="no getter available", level=DeprecationLevel.ERROR)
    var condition : (Pair<Float, Float>, Pair<Float, Float>) -> Boolean
        set(value) { swipeDetector.condition = value }
        get() = {_, _ -> false }

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
    private var onTop = true
    private var toast: Toast? = null
    private var devicesDataArray : Array<DisplayedCountersData> ?= null
    private val swipeDetector = DownSwipe()
    private val dataServer = DataServer()
    private lateinit var progressBar : ProgressBar
    private lateinit var deviceStore : DeviceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setIcon(this)
        init()
        deviceStore = DeviceStore(this, dataServer)
        swipeDetector.action = {
            toast = Toast.makeText(this, R.string.loading_message, Toast.LENGTH_SHORT)
            toast?.show()
            loadList()
        }
        swipeDetector.condition = { point1, point2 ->
            val deltaX = point1.first - point2.first
            val deltaY = point1.second - point2.second

            (point1.second < 50 && abs(deltaX) < 100 && deltaY < -700)
        }
        swipeDetector.swipeIsRelevant = { this.onTop }
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView?.layoutManager = linearLayoutManager
        recyclerView?.setOnTouchListener { view, event -> swipeDetector.onTouch(view, event) }
        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onTop = (!recyclerView.canScrollVertically(-1) && dy <= 0)
            }
        })
        val data = restoreArrayOfDisplayedCounterData(savedInstanceState)
        if (data == null) {
            setProgressBarVisible()
            loadList()
        }
        else {
            devicesDataArray = data
            recyclerView?.adapter = DeviceAdapter(data, this, swipeDetector)
        }
    }

    private fun init() {
        progressBar = findViewById(R.id.progressBarMain)
        recyclerView = findViewById(R.id.devicesList)
    }

    private fun saveArrayOfDisplayedCounterData(data : Array<DisplayedCountersData>, bundle : Bundle) {
        bundle.putParcelableArray(listDevicesParameter, data)
    }

    private fun restoreArrayOfDisplayedCounterData(bundle : Bundle?) : Array<DisplayedCountersData>? {
        val savedData = bundle?.getParcelableArray(listDevicesParameter,
                            DisplayedCountersData::class.java) ?: return null
        val countDownLatch = CountDownLatch(1)
        Executors.newSingleThreadExecutor().execute {
            savedData.forEach { it.retrieveBitmap(deviceStore) }
            countDownLatch.countDown()
        }
        countDownLatch.await()
        return savedData
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val devicesArray = devicesDataArray ?: return
        saveArrayOfDisplayedCounterData(devicesArray, outState)
   }

    private fun loadList() {
        Executors.newSingleThreadExecutor().execute {
            devicesDataArray = DisplayedCountersData.get(deviceStore, dataServer)
            runOnUiThread {
                val isEmpty = devicesDataArray?.isEmpty() ?: return@runOnUiThread
                if (isEmpty) {
                    askIfRetry(this) { this@MainActivity.loadList() }
                } else {
                    val data = devicesDataArray ?: return@runOnUiThread
                    recyclerView?.adapter = DeviceAdapter(data, this, swipeDetector)
                    toast?.cancel()
                    toast = null
                    setRecyclerViewVisible()
                }
            }
            Log.d("test output", "start populating pictures")
            val array = devicesDataArray ?: return@execute
            for (i in array.indices) {
                val pictureURL = array[i].pictureURL ?: continue
                val counterDevice = BikeCounterDevice(array[i].name, array[i].address, pictureURL)
                val picture = deviceStore.updateImages(counterDevice) ?: continue
                runOnUiThread {
                    array[i].picture = picture
                    recyclerView?.adapter?.notifyItemChanged(i)
                }
            }
        }
    }

    private fun setProgressBarVisible() {
        progressBar.visibility = View.VISIBLE
        recyclerView?.visibility = View.INVISIBLE
    }

    private fun setRecyclerViewVisible() {
        progressBar.visibility = View.INVISIBLE
        recyclerView?.visibility = View.VISIBLE
    }
}
