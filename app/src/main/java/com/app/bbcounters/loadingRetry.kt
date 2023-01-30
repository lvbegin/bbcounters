package com.app.bbcounters

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface


fun askIfRetry(activity: Activity, cb : () -> Unit) {
    activity.runOnUiThread {
        AlertDialog.Builder(activity).setTitle("Error")
            .setIcon(R.drawable.ic_launcher_foreground)
            .setMessage("Cannot read data. Retry?")
            .setPositiveButton("YES", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which : Int) {
                    cb()
                }
            })
            .setNegativeButton("NO", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    activity.finish()
                }
            }).create().show()
    }
}