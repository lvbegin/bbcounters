package com.app.bbcounters

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData

fun LineChart.configure() {
    this.xAxis.position = XAxis.XAxisPosition.BOTTOM

    this.xAxis.setDrawGridLines(false)
    this.xAxis.setDrawLabels(false)
    this.axisLeft.setDrawLabels(false)
    this.axisLeft.setDrawGridLines(false)
    this.axisRight.setDrawGridLines(false)
    this.axisRight.textSize = 12f
}

fun LineChart.configureNoDataMessage() {
    this.setNoDataText(resources.getString(R.string.loading_data))
    this.setNoDataTextColor(R.color.primaryTextColor)
}

fun LineChart.drawData(lineData: LineData)
{
    this.data = lineData
    this.notifyDataSetChanged()
    this.invalidate()
}