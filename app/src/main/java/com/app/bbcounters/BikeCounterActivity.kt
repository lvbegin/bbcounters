package com.app.bbcounters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.concurrent.Executors

class HistoryAdapter(private val history : MutableMap<String, Int>,
                    private val context : Context) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    val years = history.keys.sorted().toTypedArray()

    class HistoryViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_counter, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val year = years.get(position)
        val number = history.get(year);
        val yearTextView = holder.view.findViewById<TextView>(R.id.historyYear);
        val numberTextView = holder.view.findViewById<TextView>(R.id.historyValue)
        yearTextView.text = year
        numberTextView.text = number.toString()
    }

    override fun getItemCount(): Int = history.count()
}

class BikeCounterActivity : AppCompatActivity(),  GestureDetector.OnGestureListener{
    private lateinit var barchart : BarChart
    private lateinit var detector : GestureDetectorCompat
    private lateinit var id: String

    companion object {
        fun startActivity(context: Context, id : String) {
            val intent = Intent(context, BikeCounterActivity::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bike_counter)
        setIcon(this)
        detector = GestureDetectorCompat(this,  this)

        barchart = findViewById(R.id.counterHistoryChart)
        barchart.setNoDataText(resources.getString(R.string.loading_data))
        barchart.setNoDataTextColor(R.color.primaryTextColor)
        barchart.setOnTouchListener( object : View.OnTouchListener {
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean = detector.onTouchEvent(p1)
        })

        val idIn = intent.extras?.getString("id")
        if (idIn == null)
            return
        id = idIn
        loadDataIntoGraph()
    }

    private fun loadDataIntoGraph() {
        Executors.newSingleThreadExecutor().execute {
            val historyData = DataServer().getCounterHistory(id)
            if (historyData.isEmpty())
            {
                askIfRetry(this@BikeCounterActivity) { this@BikeCounterActivity.loadDataIntoGraph() }
                return@execute
            }
            var years = ArrayList<BarEntry>()
            for (year in historyData.keys) {
                val y : Float = Integer.parseInt(year).toFloat()
                val value : Float = historyData.get(year)!!.toFloat()
                years.add(BarEntry(y, value))
            }
            val barDataSet = BarDataSet(years, getString(R.string.bike_counter_label))
            val barData = BarData(barDataSet)
            barDataSet.colors = ColorTemplate.JOYFUL_COLORS.toList()
            barDataSet.valueTextColor = Color.BLACK
            barDataSet.valueTextSize = 15f
            barchart.description = null
            barchart.xAxis.valueFormatter = object : IAxisValueFormatter {
                override fun getFormattedValue(value : Float, axisBase : AxisBase) : String {
                    val intValue = value.toInt()
                    return if (intValue.toFloat().equals(value)) intValue.toString() else ""
                }
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
        if (p0.y - p1.y > 50)
            YearCounterActivity.startActivity(this, id)
        return true
    }

}