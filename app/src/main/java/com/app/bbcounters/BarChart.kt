package com.app.bbcounters

import com.github.mikephil.charting.charts.BarChart

fun BarChart.setProportional() {
    this.axisRight.axisMinimum = 0f
    this.axisLeft.axisMinimum = 0f
}