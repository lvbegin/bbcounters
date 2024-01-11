package com.app.bbcounters

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData

fun BarChart.setProportional() {
    this.axisRight.axisMinimum = 0f
    this.axisLeft.axisMinimum = 0f
}

fun BarChart.drawData(barData : BarData) {
    this.data = barData
    this.notifyDataSetChanged()
    this.invalidate()
}

fun BarChart.configure() {
    this.xAxis.granularity = 1f
    this.xAxis.position = XAxis.XAxisPosition.BOTTOM
    this.xAxis.textSize = 12f
    this.xAxis.setDrawGridLines(false)
    this.axisRight.setDrawLabels(false)
    this.axisLeft.setDrawLabels(false)
    this.axisLeft.setDrawGridLines(false)
    this.axisRight.setDrawGridLines(false)
}

fun BarChart.configureNoDataMessage() {
    this.setNoDataText(resources.getString(R.string.loading_data))
    this.setNoDataTextColor(R.color.primaryTextColor)
}