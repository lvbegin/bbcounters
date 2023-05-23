package com.app.bbcounters

import android.app.Activity
import android.app.AlertDialog

fun askIfRetry(activity: Activity, cb : () -> Unit) {
    activity.runOnUiThread {
        AlertDialog.Builder(activity).setTitle("Error")
            .setIcon(R.drawable.ic_launcher_foreground)
            .setMessage("Cannot read data. Retry?")
            .setPositiveButton("YES"
            ) { _, _ -> cb() }
            .setNegativeButton("NO"
            ) { _, _ -> activity.finish() }.create().show()
    }
}