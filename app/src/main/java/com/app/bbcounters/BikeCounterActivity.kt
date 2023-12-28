package com.app.bbcounters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Calendar
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.abs
import androidx.activity.OnBackPressedCallback
import java.util.stream.IntStream

class ParcelableBarEntry(v1 : Float, v2 : Float) : BarEntry(v1, v2), Parcelable {
    constructor(parcel: Parcel) : this(parcel.readFloat(), parcel.readFloat())

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
    private lateinit var progressbar : ProgressBar
    private lateinit var progressbar2 : ProgressBar
    private lateinit var progressBarText : TextView
    private lateinit var id: String
    private var swipeLeft = false
    private var firstYear = 2018
    private var thisYear  = 0

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
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_rigth)
            }
        })
        swipeDetector.action  = {
            if (swipeLeft)
            {
                YearCounterActivity.startActivity(this, id)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            else
            {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_rigth)
            }
        }
        swipeDetector.condition = { point1, point2 ->
            val deltaX =  point1.first - point2.first
            val deltaY = point1.second - point2.second
            if (abs(deltaX) > 300 && abs(deltaY) < 200)
            {
                swipeLeft = (deltaX > 0)
                true
            }
            else false
        }
        id = intent.extras?.getString(deviceIdParameter) ?: return
        listYears = savedInstanceState?.getStringArrayList(listYearSavedState)
        thisYear  = Calendar.getInstance().get(Calendar.YEAR)
        progressbar = findViewById(R.id.progressBarBikeCounter)
        progressbar2 = findViewById(R.id.progressBarBikeCounter2)
        progressBarText = findViewById(R.id.progressBarText)
        barchart = findViewById(R.id.counterHistoryChart)
        barchart.setNoDataText(resources.getString(R.string.loading_data))
        barchart.setNoDataTextColor(R.color.primaryTextColor)
        barchart.setOnTouchListener(swipeDetector)
        barchart.setProportional()
        setProgressBarVisible()
        val values : ArrayList<ParcelableBarEntry>? = savedInstanceState?.getParcelableArrayList(graphValuesSavedState, ParcelableBarEntry::class.java)
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
        progressbar.max = thisYear - firstYear
        progressbar.progress = 0
        Executors.newSingleThreadExecutor().execute {
            if (listYears == null || yearValues == null)
            {
                val years = ArrayList<String>()
                val values = mutableListOf<ParcelableBarEntry>()
                listYears = years
                yearValues = values
                (firstYear..thisYear).forEach() { i ->
                    runOnUiThread {
                        progressBarText.text = getString(R.string.loading_all_year_data, i.toString())
                    }
                    val data = DataServer().getCounterHistoryYear(id, i.toString())
                    data.onFailure {
                        askIfRetry(this@BikeCounterActivity) { this@BikeCounterActivity.loadDataIntoGraph() }
                        return@execute
                    }
                    data.onSuccess {
                        val value = it.values.stream().reduce(0, Integer::sum)
                        if (value > 0)
                        {
                            values.add(ParcelableBarEntry(i.toFloat(), value.toFloat()))
                            years.add(i.toString())
                            Log.v("test output", values.toString())
                        }
                        runOnUiThread {
                            progressbar.progress++
                        }
                    }
                }
            }
            updateGraph()
        }
    }
    private fun updateGraph() {
        val values = yearValues as List<BarEntry>
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
        barchart.configure()
        runOnUiThread {
            setBarChartVisible()
            barchart.drawData(barData)
        }
    }

    private fun setProgressBarVisible() {
        progressbar.visibility = View.VISIBLE
        progressbar2.visibility = View.VISIBLE
        progressBarText.visibility = View.VISIBLE
        barchart.visibility = View.INVISIBLE
    }

    private fun setBarChartVisible() {
        progressbar.visibility = View.INVISIBLE
        progressbar2.visibility = View.INVISIBLE
        progressBarText.visibility = View.INVISIBLE
        barchart.visibility = View.VISIBLE
    }
}
