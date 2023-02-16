package com.app.bbcounters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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

class YearCounterActivity : android.support.v7.app.AppCompatActivity() {
    private lateinit var lineChart: LineChart
    private lateinit var id : String
    private lateinit var listYears : ArrayList<String>

    companion object {
        fun startActivity(context: Context, id : String, listYears: ArrayList<String>) {
            val intent = Intent(context, YearCounterActivity::class.java)
            intent.putExtra("id", id)
            intent.putExtra("years", listYears)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_year_counter)
        setIcon(this)
        id = intent.extras?.getString("id") ?: return
        listYears = intent.getSerializableExtra("years") as ArrayList<String>
        listYears.sort()
        lineChart = findViewById<LineChart>(R.id.yearlyHistoryChart)
        lineChart.setNoDataText(resources.getString(R.string.loading_data))
        lineChart.setNoDataTextColor(R.color.primaryTextColor)
        val spinner = findViewById<Spinner>(R.id.list_counter_years)
        val adapter = ArrayAdapter<String>(this, R.layout.year_item_layout)
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        for (year in listYears) {
            adapter.add(year)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val year : String = adapter.getItem(position) ?: return
                this@YearCounterActivity.loadDataIntoGraph(year)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        spinner.adapter = adapter
        spinner.setSelection(listYears.size - 1)

    }

    fun loadDataIntoGraph(year : String) {
        lineChart.clear()
        lineChart.notifyDataSetChanged()
        lineChart.invalidate()
        Executors.newSingleThreadExecutor().execute {

            var data = DataServer().getCounterHistoryYear(id, year)
            if (data.isEmpty()) {
                askIfRetry(this) { this@YearCounterActivity.loadDataIntoGraph(year) }
                return@execute
            }

            var keys = data.keys.toTypedArray()

            val entries = IntStream.range(0, data.keys.size).mapToObj { i -> Entry(i.toFloat(),
                    data.get(keys.get(i))?.toFloat() ?: 0f) }.collect(Collectors.toList())
            val dayValues = ArrayList<Entry>(entries)
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
                lineChart.notifyDataSetChanged()
                lineChart.invalidate()
            }

        }

    }
}