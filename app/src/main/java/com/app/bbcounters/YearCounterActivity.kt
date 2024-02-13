package com.app.bbcounters

import android.app.Activity
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
import androidx.activity.OnBackPressedCallback
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Calendar
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.abs

class ParcelableMap : Parcelable {
    var map : MutableMap<String, Int>
    private set

    constructor(parcel: Parcel)  {
        this.map = mutableMapOf()
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
        parcel.writeInt(map.size)
        map.keys.forEach {
            val value = map[it]
            if (value != null) {
                parcel.writeString(it)
                parcel.writeInt(value)
            }
        }
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ParcelableMap> {
        override fun createFromParcel(parcel: Parcel) = ParcelableMap(parcel)

        override fun newArray(size: Int): Array<ParcelableMap?> = arrayOfNulls(size)
    }
}

class YearCounterActivity : androidx.appcompat.app.AppCompatActivity() {
    private val currentYearSavedState : String = "current"
    private val dataSavedState : String = "data"
    private val lineGraphTypeSavedState : String = "lineGraphType"
    private var lineChart: LineChart? = null
    private var barChart: BarChart? = null
    private var dataFromServer : ParcelableMap? = null
    private val latchInitSelectedYear = CountDownLatch(1)
    private lateinit var id : String
    private var listYears : ArrayList<String>? = null
    private lateinit var selectedYear : String
    private lateinit var yearSpinner : Spinner
    private lateinit var graphTypeSpinner : Spinner
    private lateinit var progressBar : ProgressBar
    private var currentYearIndex : Int? = null
    private var lineGraphType = false
    private val basicSwipe = BasicSwipe()
    companion object {
        private const val deviceIdParameter : String = "id"
        private const val yearParameter : String = "years"
        fun startActivity(context: Activity, id : String) {
            val intent = Intent(context, YearCounterActivity::class.java)
            intent.putExtra(deviceIdParameter, id)
            context.startActivity(intent)
            context.setScrollingAnimationRightToLeft()
        }
    }

    private fun initYearSpinnerInExecutor() {
        if (latchInitSelectedYear.count > 0)
        {
            val adapter = ArrayAdapter<String>(this, R.layout.year_item_layout)
            adapter.setDropDownViewResource(R.layout.year_item_list_layout)
            listYears?.forEach {  adapter.add(it) }
            yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                    if (this@YearCounterActivity.currentYearIndex != position) {
                        selectedYear = adapter.getItem(position) ?: return
                        this@YearCounterActivity.currentYearIndex = position
                        dataFromServer = null
                        this@YearCounterActivity.loadDataIntoGraph()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) { }
            }
            runOnUiThread {
                val years = listYears  ?: return@runOnUiThread
                val yearIndex = years.size - 1
                yearSpinner.adapter = adapter
                yearSpinner.setSelection(yearIndex)
                currentYearIndex = yearIndex
                selectedYear = years[yearIndex]
                latchInitSelectedYear.countDown()
            }
            latchInitSelectedYear.await()
        }
    }

    private fun init(savedInstanceState: Bundle?) {
        yearSpinner = findViewById(R.id.list_counter_years)
        graphTypeSpinner = findViewById(R.id.graph_type_years)
        lineChart = findViewById(R.id.yearlyHistoryLineChart)
        barChart = findViewById(R.id.yearlyHistoryBarChart)
        progressBar = findViewById(R.id.progressBarYearBikeCounter)
        lineGraphType = savedInstanceState?.getBoolean(lineGraphTypeSavedState) ?: false
        dataFromServer = savedInstanceState?.getParcelable(dataSavedState, ParcelableMap::class.java)
        id = intent.extras?.getString(deviceIdParameter) ?: return
        listYears = savedInstanceState?.getStringArrayList("listYears")
    }

    private fun initNavigation() {
        basicSwipe.action  = { finishWithScrollingLeftToRight() }
        basicSwipe.condition = { point1, point2 ->
            val deltaX =  point2.first - point1.first
            val deltaY = point1.second - point2.second
            (deltaX > 300 && abs(deltaY) < 100)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finishWithScrollingLeftToRight()
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_year_counter)
        setIcon(this)
        init(savedInstanceState)
        initNavigation()
        val adapterGraphType = ArrayAdapter(this, R.layout.year_graph_item_layout, resources.getStringArray(R.array.graph_type))
        adapterGraphType.setDropDownViewResource(R.layout.year_graph_item_list_layout)
        graphTypeSpinner.adapter = adapterGraphType
        graphTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isLineGraph = position == 1
                if (this@YearCounterActivity.lineGraphType != isLineGraph) {
                    val yearAdapter = yearSpinner.adapter as ArrayAdapter<String>
                    val currentYearIndex = this@YearCounterActivity.currentYearIndex ?: return
                    selectedYear = yearAdapter.getItem(currentYearIndex) ?: return
                    this@YearCounterActivity.lineGraphType = isLineGraph
                    dataFromServer = null
                    this@YearCounterActivity.loadDataIntoGraph()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        lineChart?.configureNoDataMessage()
        lineChart?.setOnTouchListener(basicSwipe)
        barChart?.configureNoDataMessage()
        barChart?.setOnTouchListener(basicSwipe)
        loadDataIntoGraph()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val savedCurrentYearIndex = currentYearIndex
        val savedData = dataFromServer
        if (savedCurrentYearIndex != null)
            outState.putInt(currentYearSavedState, savedCurrentYearIndex)
        if (savedData != null)
            outState.putParcelable(dataSavedState, savedData)
        outState.putStringArrayList("listYears", listYears)
        outState.putBoolean(lineGraphTypeSavedState, lineGraphType)
    }

    fun loadDataIntoGraph() {
        if (lineGraphType)
            loadDataIntoLineGraph()
        else
            loadDataIntoBarGraph()
    }
    private fun loadDataIntoLineGraph() {
        setProgressBarVisible()
        lineChart?.clear()
        Executors.newSingleThreadExecutor().execute {
            if (listYears == null)
                listYears = yearOfActivityOfCounter(this.id)
            initYearSpinnerInExecutor()
            val year = selectedYear
            if (dataFromServer == null) {
                val data = DataServer().getCounterHistoryYear(id, year)
                data.onFailure {
                    askIfRetry(this) { this@YearCounterActivity.loadDataIntoLineGraph() }
                    return@execute
                }
                data.onSuccess {
                    dataFromServer = ParcelableMap(it)
                }
            }
            val data = dataFromServer?.map ?: return@execute
            val keys = data.keys.toTypedArray()

            val entriesLine = IntStream.range(0, data.keys.size).mapToObj { i ->
                Entry(i.toFloat(),
                    data[keys[i]]?.toFloat() ?: 0f)
            }.collect(Collectors.toList())
            val dayValues = entriesLine as List<Entry>
            val lineDataSet = LineDataSet(dayValues, getString(R.string.graph_year_counter_label))
            lineDataSet.setDrawValues(false)
            lineDataSet.color = Color.BLUE
            lineDataSet.circleColors = MutableList(1) { Color.BLUE }
            lineChart?.configure()

            val lineData = LineData(lineDataSet)

            runOnUiThread {
                setLineChartVisible()
                lineChart?.drawData(lineData)
            }

        }
    }

    private fun loadDataIntoBarGraph() {
        setProgressBarVisible()
        barChart?.clear()
        Executors.newSingleThreadExecutor().execute {
            if (listYears == null)
                listYears = yearOfActivityOfCounter(this.id)
            initYearSpinnerInExecutor()
            val year = selectedYear
            if (dataFromServer == null) {
                val data = DataServer().getCounterHistoryYear(id, year)
                data.onFailure {
                    askIfRetry(this) { this@YearCounterActivity.loadDataIntoLineGraph() }
                    return@execute
                }
                data.onSuccess {
                    dataFromServer = ParcelableMap(it)
                }
            }
            val data = dataFromServer?.map ?: return@execute
            val keys = data.keys.toTypedArray()

            val entriesBar = IntStream.range(0, data.keys.size).mapToObj { i ->
                Pair(
                    keys[i].subSequence(5, 7).toString().toFloat(),
                    data[keys[i]] ?: 0
                )
            }.collect(Collectors.toList())
            val monthMap = mutableMapOf<Float, Int>()
            val entriesBarList = entriesBar ?: return@execute
            entriesBarList.forEach {
                monthMap[it.first] = monthMap.getOrDefault(it.first, 0) + it.second
            }
            val monthValues = monthMap.keys.stream().map { month ->
                val value: Float = monthMap[month]!!.toFloat()
                ParcelableBarEntry(month, value)
            }.collect(Collectors.toList())
            val values = monthValues as List<BarEntry>
            val barDataSet = BarDataSet(values, getString(R.string.year_counter_label))
            val barData = BarData(barDataSet)
            barDataSet.colors = ColorTemplate.JOYFUL_COLORS.toList()
            barDataSet.valueTextColor = Color.BLACK
            barDataSet.valueTextSize = 15f
            barChart?.xAxis?.valueFormatter = IAxisValueFormatter { value, _ ->
                val xAxis = arrayListOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                val intValue = value.toInt()
                if (intValue.toFloat().equals(value)) xAxis[intValue - 1] else ""
            }
            barChart?.description = null
            barChart?.configure()
            barChart?.setProportional()
            runOnUiThread {
                setBarChartVisible()
                barChart?.drawData(barData)
            }
        }
    }

    private  fun yearOfActivityOfCounter(name : String) : ArrayList<String>  {
        val thisYear  = Calendar.getInstance().get(Calendar.YEAR)
        val server = DataServer()
        val firstYear = IntStream.range(DataServer.firstYear, thisYear + 1).filter {
                i ->
            i == thisYear ||
                    server.CounterExisted(
                        name,
                        i.toString()
                    )
        }.findFirst().asInt
        return IntStream.range(firstYear, thisYear + 1).mapToObj { i -> i.toString() }
            .toArray()
            .toCollection(ArrayList()) as ArrayList<String>
    }

    private fun setBarChartVisible() {
        yearSpinner.visibility = View.VISIBLE
        graphTypeSpinner.visibility = View.VISIBLE
        lineChart?.visibility = View.INVISIBLE
        barChart?.visibility = View.VISIBLE
        progressBar.visibility = View.INVISIBLE
        yearSpinner.isEnabled = true
        graphTypeSpinner.isEnabled = true
    }

    private fun setLineChartVisible() {
        yearSpinner.visibility = View.VISIBLE
        graphTypeSpinner.visibility = View.VISIBLE
        lineChart?.visibility = View.VISIBLE
        barChart?.visibility = View.INVISIBLE
        progressBar.visibility = View.INVISIBLE
        yearSpinner.isEnabled = true
        graphTypeSpinner.isEnabled = true
    }

    private fun setProgressBarVisible() {
        if (latchInitSelectedYear.count > 0) {
            yearSpinner.visibility = View.INVISIBLE
            graphTypeSpinner.visibility = View.INVISIBLE
        }
        else {
            yearSpinner.visibility = View.VISIBLE
            graphTypeSpinner.visibility = View.VISIBLE
        }
        lineChart?.visibility = View.INVISIBLE
        barChart?.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE
        yearSpinner.isEnabled = false
        graphTypeSpinner.isEnabled = false
    }
}