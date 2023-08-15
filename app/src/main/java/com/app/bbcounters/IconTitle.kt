package com.app.bbcounters

import androidx.appcompat.app.AppCompatActivity

fun setIcon(activity : AppCompatActivity) {
    val actionBar = activity.supportActionBar
    if (actionBar != null) {
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setIcon(R.mipmap.ic_launcher_round)
    }
}
