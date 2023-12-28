package com.app.bbcounters

import android.app.Activity
import android.app.AlertDialog

fun graphShortcutCallback(context: Activity, data : Array<DisplayedCountersData>, position : Int,
                          x : Int, y : Int, gravity : Int) : Boolean {
    val a = arrayOf("History", "Year")
    val builder = AlertDialog.Builder(context, R.style.dialog_with_rounded_corner)
        .setItems(a) { _, which ->
            when (which) {
                0 -> {
                    BikeCounterActivity.startActivity(context, data[position].name)
                    context.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                1-> {
                    YearCounterActivity.startActivity(context, data[position].name)
                    context.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                else -> Log.e("test output", "Unknown choice selected")
            }
        }
    val dialog = builder.create()
    dialog.window?.attributes?.gravity = gravity
    dialog.window?.attributes?.x = x
    dialog.window?.attributes?.y = y

    dialog.show()
    dialog.window?.setLayout(500, 400)
    return true
}
