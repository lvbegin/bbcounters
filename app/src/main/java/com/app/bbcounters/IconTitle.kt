package com.app.bbcounters

import android.support.v7.app.AppCompatActivity

fun setIcon(activity : AppCompatActivity) {
    val actionBar = activity.supportActionBar
    if (actionBar != null) {
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setIcon(R.mipmap.ic_launcher_round)
    }
}
