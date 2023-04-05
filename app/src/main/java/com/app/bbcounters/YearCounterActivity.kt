package com.app.bbcounters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.concurrent.Executors
import java.util.stream.Collectors
import java.util.stream.IntStream

class ParcelableMap : Parcelable {
    var map : MutableMap<String, Int>
    private set

    constructor(parcel: Parcel)  {
        this.map = mutableMapOf<String, Int>()
        val size = parcel.readInt()
        (1..size).forEach { _ ->
            val key = parcel.readString()
            val value = parcel.readInt()
            if (key != null)
                this.map[key] = value
        }
    }

    constructor(map : MutableMap<String, Int>) { this.map = map }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(this.map.size)
        for (key in map.keys) {
            parcel.writeString(key)
            parcel.writeInt(map[key]!!)
        }
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ParcelableMap> {
        override fun createFromParcel(parcel: Parcel) = ParcelableMap(parcel)

        override fun newArray(size: Int): Array<ParcelableMap?> = arrayOfNulls(size)
    }
}

class YearCounterActivity : android.support.v7.app.AppCompatActivity() {
    private val currentYearSavedState : String = "current"
    private val dataSavedState : String = "data"
    private val lineGraphTypeSavedState : String = "lineGraphType"
    private var lineChart: LineChart? = null
    private var barChart: BarChart? = null
    private var dataFromServer : ParcelableMap? = null
    private lateinit var id : String
    private lateinit var listYears : ArrayList<String>
    private lateinit var yearSpinner : Spinner
    private lateinit var graphTypeSpinner : Spinner
    private lateinit var progressBar : ProgressBar
    private var currentYearIndex : Int? = null;
    private var lineGraphType = false
    companion object {
        private const val deviceIdParameter : String = "id"
        private const val yearParameter : String = "years"
        fun startActivity(context: Context, id : String, listYears: ArrayList<String>) {
            val intent = Intent(context, YearCounterActivity::class.java)
            intent.putExtra(deviceIdParameter, id)
            intent.putExtra(yearParameter, listYears)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_year_counter)
        setIcon(this)
        lineGraphType = savedInstanceState?.getBoolean(lineGraphTypeSavedState) ?: false
        dataFromServer = savedInstanceState?.getParcelable(dataSavedState)
        id = intent.extras?.getString(deviceIdParameter) ?: return
        listYears = intent.getSerializableExtra(yearParameter) as ArrayList<String>
        listYears.sort()
        val yearIndex = savedInstanceState?.getInt(currentYearSavedState) ?: listYears.size - 1
        val adapter = ArrayAdapter<String>(this, R.layout.year_item_layout)
        adapter.setDropDownViewResource(R.layout.year_item_list_layout)
        listYears.forEach {  adapter.add(it) }
        yearSpinner = findViewById<Spinner>(R.id.list_counter_years)
        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val year : String = adapter.getItem(position) ?: return
                if (this@YearCounterActivity.currentYearIndex != position) {
                    this@YearCounterActivity.currentYearIndex = position
                    dataFromServer = null
                    this@YearCounterActivity.loadDataIntoGraph(year)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        yearSpinner.adapter = adapter
        val adapterGraphType = ArrayAdapter(this, R.layout.year_graph_item_layout, resources.getStringArray(R.array.graph_type))
        graphTypeSpinner = findViewById<Spinner>(R.id.graph_type_years)
        adapterGraphType.setDropDownViewResource(R.layout.year_graph_item_list_layout)
        graphTypeSpinner.adapter = adapterGraphType
        graphTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isLineGraph = position == 1
                val yearAdapter = yearSpinner.adapter as ArrayAdapter<String>
                val currentYearIndex = this@YearCounterActivity.currentYearIndex ?: return
                val year = yearAdapter.getItem(currentYearIndex) ?: return
                if (this@YearCounterActivity.lineGraphType != isLineGraph) {
                    this@YearCounterActivity.lineGraphType = isLineGraph
                    dataFromServer = null
                    this@YearCounterActivity.loadDataIntoGraph(year)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        currentYearIndex = yearIndex
        yearSpinner.setSelection(yearIndex)
        lineChart = findViewById<LineChart>(R.id.yearlyHistoryLineChart)
        lineChart?.setNoDataText(resources.getString(R.string.loading_data))
        lineChart?.setNoDataTextColor(R.color.primaryTextColor)
        barChart = findViewById<BarChart>(R.id.yearlyHistoryBarChart)
        barChart?.setNoDataText(resources.getString(R.string.loading_data))
        barChart?.setNoDataTextColor(R.color.primaryTextColor)
        progressBar = findViewById(R.id.progressBarYearBikeCounter)
        loadDataIntoGraph(listYears[yearIndex])
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val savedCurrentYearIndex = currentYearIndex
        val savedData = dataFromServer
        if (savedCurrentYearIndex != null)
            outState.putInt(currentYearSavedState, savedCurrentYearIndex)
        if (savedData != null)
            outState.putParcelable(dataSavedState, savedData)
        outState.putBoolean(lineGraphTypeSavedState, lineGraphType)
    }

    fun loadDataIntoGraph(year : String) {
        if (lineGraphType)
            loadDataIntoLineGraph(year)
        else
            loadDataIntoBarGraph(year)
    }
    fun loadDataIntoLineGraph(year : String) {
        barChart?.visibility = View.INVISIBLE
        lineChart?.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE
        lineChart?.clear()
        yearSpinner.isEnabled = false
        graphTypeSpinner.isEnabled = false
        Executors.newSingleThreadExecutor().execute {
            if (dataFromServer == null) {
                var data = DataServer().getCounterHistoryYear(id, year)
                if (data.isFailure) {
                    askIfRetry(this) { this@YearCounterActivity.loadDataIntoLineGraph(year) }
                    return@execute
                }
                data.onSuccess {
                    dataFromServer = ParcelableMap(it)
                }
            }
            var data = dataFromServer?.map ?: return@execute
            var keys = data.keys.toTypedArray()

            val entriesLine = IntStream.range(0, data.keys.size).mapToObj { i ->
                Entry(
                    i.toFloat(),
                    data[keys[i]]?.toFloat() ?: 0f
                )
            }.collect(Collectors.toList())
            val dayValues = entriesLine as List<Entry>
            var lineDataSet = LineDataSet(dayValues, getString(R.string.graph_year_counter_label))
            lineDataSet.setDrawValues(false)
            lineDataSet.color = Color.BLUE
            lineDataSet.circleColors = MutableList(1) { Color.BLUE }
            lineChart?.xAxis?.position = XAxis.XAxisPosition.BOTTOM

            lineChart?.xAxis?.setDrawGridLines(false)
            lineChart?.xAxis?.setDrawLabels(false)
            lineChart?.axisLeft?.setDrawLabels(false)
            lineChart?.axisLeft?.setDrawGridLines(false)
            lineChart?.axisRight?.setDrawGridLines(false)
            lineChart?.axisRight?.textSize = 12f

            var lineData = LineData(lineDataSet)

            runOnUiThread {
                lineChart?.visibility = View.VISIBLE
                barChart?.visibility = View.INVISIBLE
                progressBar.visibility = View.INVISIBLE
                lineChart?.data = lineData
                yearSpinner.isEnabled = true
                graphTypeSpinner.isEnabled = true
                lineChart?.notifyDataSetChanged()
                lineChart?.invalidate()
            }

        }
    }
    fun loadDataIntoBarGraph(year : String) {
        barChart?.visibility = View.INVISIBLE
        lineChart?.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE
        barChart?.clear()
        yearSpinner.isEnabled = false
        graphTypeSpinner.isEnabled = false
        Executors.newSingleThreadExecutor().execute {
            if (dataFromServer == null) {
                var data = DataServer().getCounterHistoryYear(id, year)
                if (data.isFailure) {
                    askIfRetry(this) { this@YearCounterActivity.loadDataIntoLineGraph(year) }
                    return@execute
                }
                data.onSuccess {
                    dataFromServer = ParcelableMap(it)
                }
            }
            var data = dataFromServer?.map ?: return@execute
            var keys = data.keys.toTypedArray()

            val entriesBar = IntStream.range(0, data.keys.size).mapToObj { i ->
                Pair<Float, Int>(
                    keys[i].subSequence(5, 7).toString().toFloat(),
                    data[keys[i]]?.toInt() ?: 0
                )
            }.collect(Collectors.toList())
            var monthMap = mutableMapOf<Float, Int>()
            val entriesBarList = entriesBar ?: return@execute
            entriesBarList.forEach {
                monthMap[it.first] = monthMap.getOrDefault(it.first, 0) + it.second
            }
            val monthValues = monthMap.keys.stream().map { month ->
                val value: Float = monthMap.get(month)!!.toFloat()
                ParcelableBarEntry(month, value)
            }.collect(Collectors.toList())
            var values = monthValues as List<BarEntry>
            val barDataSet = BarDataSet(values, getString(R.string.year_counter_label))
            val barData = BarData(barDataSet)
            barDataSet.colors = ColorTemplate.JOYFUL_COLORS.toList()
            barDataSet.valueTextColor = Color.BLACK
            barDataSet.valueTextSize = 15f
            barChart?.xAxis?.valueFormatter = IAxisValueFormatter { value, _ ->
                val xAxis = arrayListOf<String>("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                val intValue = value.toInt()
                if (intValue.toFloat().equals(value)) xAxis[intValue - 1] else ""
            }
            barChart?.description = null
            barChart?.xAxis?.granularity = 1f
            barChart?.xAxis?.position = XAxis.XAxisPosition.BOTTOM
            barChart?.xAxis?.textSize = 12f
            barChart?.xAxis?.setDrawGridLines(false)
            barChart?.axisRight?.setDrawLabels(false)
            barChart?.axisLeft?.setDrawLabels(false)
            barChart?.axisLeft?.setDrawGridLines(false)
            barChart?.axisRight?.setDrawGridLines(false)
            runOnUiThread {
                barChart?.visibility = View.VISIBLE
                lineChart?.visibility = View.INVISIBLE
                progressBar.visibility = View.INVISIBLE
                barChart?.data = barData
                yearSpinner.isEnabled = true
                graphTypeSpinner.isEnabled = true
                barChart?.notifyDataSetChanged()
                barChart?.invalidate()
            }
        }
    }
}