package com.app.bbcounters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.concurrent.Executors
import java.util.stream.Collectors
import java.util.stream.IntStream

class ParcelableEntry(x : Float, y : Float) : Entry(x, y), Parcelable {
    constructor(parcel: Parcel) : this(parcel.readFloat(), parcel.readFloat()) { }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableEntry> {
        override fun createFromParcel(parcel: Parcel): ParcelableEntry {
            return ParcelableEntry(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableEntry?> {
            return arrayOfNulls(size)
        }
    }

}

class YearCounterActivity : android.support.v7.app.AppCompatActivity() {
    private lateinit var lineChart: LineChart
    private lateinit var id : String
    private lateinit var listYears : ArrayList<String>
    private lateinit var spinner : Spinner
    private var entries : List<ParcelableEntry>? = null
    private var currentYearIndex : Int? = null;
    companion object {
        private const val deviceIdParameter : String = "id"
        private const val yearParameter : String = "years"
        private const val entriesSavedState : String = "entries"
        private const val currentYearSavedState : String = "current"
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
        val savedEntries : ArrayList<ParcelableEntry>? = savedInstanceState?.getParcelableArrayList(entriesSavedState)
        entries = savedEntries?.toList()

        id = intent.extras?.getString(deviceIdParameter) ?: return
        listYears = intent.getSerializableExtra(yearParameter) as ArrayList<String>
        listYears.sort()
        val yearIndex = savedInstanceState?.getInt(currentYearSavedState) ?: listYears.size - 1
        lineChart = findViewById<LineChart>(R.id.yearlyHistoryChart)
        lineChart.setNoDataText(resources.getString(R.string.loading_data))
        lineChart.setNoDataTextColor(R.color.primaryTextColor)
        val adapter = ArrayAdapter<String>(this, R.layout.year_item_layout)
        adapter.setDropDownViewResource(R.layout.year_item_list_layout)
        for (year in listYears) {
            adapter.add(year)
        }

        spinner = findViewById<Spinner>(R.id.list_counter_years)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val year : String = adapter.getItem(position) ?: return
                if (this@YearCounterActivity.currentYearIndex != position) {
                    this@YearCounterActivity.currentYearIndex = position
                    entries = null
                    this@YearCounterActivity.loadDataIntoGraph(year)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        spinner.adapter = adapter
        currentYearIndex = yearIndex
        spinner.setSelection(yearIndex)
        loadDataIntoGraph(listYears.get(yearIndex))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val savedEntries = entries
        val savedCurrentYearIndex = currentYearIndex
        if (savedEntries != null)
            outState.putParcelableArrayList(entriesSavedState, ArrayList<ParcelableEntry>(savedEntries))
        if (savedCurrentYearIndex != null)
            outState.putInt(currentYearSavedState, savedCurrentYearIndex)
    }

    fun loadDataIntoGraph(year : String) {
        lineChart.clear()
        lineChart.notifyDataSetChanged()
        lineChart.invalidate()
        spinner.isEnabled = false
        Executors.newSingleThreadExecutor().execute {

            if (entries == null) {
                var data = DataServer().getCounterHistoryYear(id, year)
                if (data.isEmpty()) {
                    askIfRetry(this) { this@YearCounterActivity.loadDataIntoGraph(year) }
                    return@execute
                }

                var keys = data.keys.toTypedArray()

                entries = IntStream.range(0, data.keys.size).mapToObj { i ->
                    ParcelableEntry(
                        i.toFloat(),
                        data.get(keys.get(i))?.toFloat() ?: 0f
                    )
                }.collect(Collectors.toList())
            }
            val dayValues = entries as List<Entry>
            var lineDataSet = LineDataSet(dayValues, getString(R.string.graph_year_counter_label))
            lineDataSet.setDrawValues(false)
            lineDataSet.color = Color.BLUE
            lineDataSet.circleColors = MutableList(1) { Color.BLUE }
            lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM

            lineChart.xAxis.setDrawGridLines(false)
            lineChart.xAxis.setDrawLabels(false)
            lineChart.axisLeft.setDrawLabels(false)
            lineChart.axisLeft.setDrawGridLines(false)
            lineChart.axisRight.setDrawGridLines(false)
            lineChart.axisRight.textSize = 12f

            var lineData = LineData(lineDataSet)

            runOnUiThread {
                lineChart.data = lineData
                spinner.isEnabled = true
                lineChart.notifyDataSetChanged()
                lineChart.invalidate()
            }

        }

    }
}