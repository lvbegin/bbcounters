package com.app.bbcounters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.concurrent.Executors

class YearCounterActivity : android.support.v7.app.AppCompatActivity() {
    private lateinit var lineChart: LineChart
    private  lateinit var id : String

    companion object {
        fun startActivity(context: Context, id : String) {
            val intent = Intent(context, YearCounterActivity::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_year_counter)
        setIcon(this)
        id = intent.extras?.getString("id") ?: return
        lineChart = findViewById<LineChart>(R.id.yearlyHistoryChart)
        lineChart.setNoDataText(resources.getString(R.string.loading_data))
        lineChart.setNoDataTextColor(R.color.primaryTextColor)
        loadDataIntoGraph()
    }

    fun loadDataIntoGraph() {
        Executors.newSingleThreadExecutor().execute {

            var data = DataServer().getCounterHistoryYear(id, "2023")
            if (data.isEmpty()) {
                askIfRetry(this) { this@YearCounterActivity.loadDataIntoGraph() }
                return@execute
            }
            var dayValues = ArrayList<Entry>()
            for (date in data.keys) {
                val x = Integer.parseInt(date.substring(5, 7) + date.substring(8)).toFloat()
                val y : Float = data.get(date)!!.toFloat()
                Log.v("test output", "x = ${x.toString()}, y = ${y.toString()}")
                dayValues.add(Entry(x, y))
            }
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