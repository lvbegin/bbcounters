package com.app.bbcounters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.view.GestureDetectorCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.concurrent.Executors
import java.util.stream.Collectors
import kotlin.math.abs

class ParcelableBarEntry(v1 : Float, v2 : Float) : BarEntry(v1, v2), Parcelable {
    constructor(parcel: Parcel) : this(parcel.readFloat(), parcel.readFloat()) { }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeFloat(x)
        parcel.writeFloat(y)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ParcelableBarEntry> {
        override fun createFromParcel(parcel: Parcel): ParcelableBarEntry = ParcelableBarEntry(parcel)

        override fun newArray(size: Int): Array<ParcelableBarEntry?> = arrayOfNulls(size)
    }
}

class BikeCounterActivity : AppCompatActivity() {
    private val listYearSavedState = "years"
    private val graphValuesSavedState = "values"
    private lateinit var barchart : BarChart
    private lateinit var detector : GestureDetectorCompat
    private lateinit var id: String
    private var listYears : ArrayList<String>? = null
    private var yearValues : List<ParcelableBarEntry>? = null
    private val swipeDetector = BasicSwipe()

    companion object {
        private const val deviceIdParameter = "id"
        fun startActivity(context: Context, id : String) {
            val intent = Intent(context, BikeCounterActivity::class.java)
            intent.putExtra(deviceIdParameter, id)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bike_counter)
        setIcon(this)
        swipeDetector.action  = {
            val years : ArrayList<String>? = listYears
            if (years != null)
                YearCounterActivity.startActivity(this, id, years)
        }
        swipeDetector.condition = { point1, point2 ->
            val deltaX =  point1.first - point2.first
            val deltaY = point1.second - point2.second
             (deltaX > 400 && abs(deltaY) < 100)
        }
        id = intent.extras?.getString(deviceIdParameter) ?: return
        listYears = savedInstanceState?.getStringArrayList(listYearSavedState)

        barchart = findViewById(R.id.counterHistoryChart)
        barchart.setNoDataText(resources.getString(R.string.loading_data))
        barchart.setNoDataTextColor(R.color.primaryTextColor)
        barchart.setOnTouchListener(swipeDetector)
        val values : ArrayList<ParcelableBarEntry>? = savedInstanceState?.getParcelableArrayList(graphValuesSavedState)
        yearValues = values?.toList()
        loadDataIntoGraph()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val savedListYears = listYears
        val savedValues = yearValues
        if (savedListYears != null && savedValues != null) {
            outState.putStringArrayList(listYearSavedState, savedListYears)
            outState.putParcelableArrayList(graphValuesSavedState, ArrayList<ParcelableBarEntry>(savedValues))
        }
    }

    private fun loadDataIntoGraph() {
        Executors.newSingleThreadExecutor().execute {
            if (listYears == null || yearValues == null)
            {
                val historyData = DataServer().getCounterHistory(id)
                if (historyData.isEmpty())
                {
                    askIfRetry(this@BikeCounterActivity) { this@BikeCounterActivity.loadDataIntoGraph() }
                    return@execute
                }
                listYears = ArrayList(historyData.keys)
                yearValues = historyData.keys.stream().map { year ->
                    val y : Float = Integer.parseInt(year).toFloat()
                    val value : Float = historyData.get(year)!!.toFloat()
                    ParcelableBarEntry(y, value)
                }.collect(Collectors.toList())
            }
            var values = yearValues as List<BarEntry>
            val barDataSet = BarDataSet(values, getString(R.string.bike_counter_label))
            val barData = BarData(barDataSet)
            barDataSet.colors = ColorTemplate.JOYFUL_COLORS.toList()
            barDataSet.valueTextColor = Color.BLACK
            barDataSet.valueTextSize = 15f
            barchart.description = null
            barchart.xAxis.valueFormatter = IAxisValueFormatter { value, _ ->
                val intValue = value.toInt()
                if (intValue.toFloat().equals(value)) intValue.toString() else ""
            }
            barchart.xAxis.granularity = 1f
            barchart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            barchart.xAxis.textSize = 12f
            barchart.xAxis.setDrawGridLines(false)
            barchart.axisRight.setDrawLabels(false)
            barchart.axisLeft.setDrawLabels(false)
            barchart.axisLeft.setDrawGridLines(false)
            barchart.axisRight.setDrawGridLines(false)
            runOnUiThread {
                barchart.data = barData
                barchart.notifyDataSetChanged()
                barchart.invalidate()
            }
        }
    }
}