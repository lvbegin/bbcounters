package com.app.bbcounters

import android.app.Activity
import android.app.AlertDialog

fun graphShortcutCallback(context: Activity, data : Array<DisplayedCountersData>, position : Int,
                          x : Int, y : Int, gravity : Int) : Boolean {
    val a = arrayOf(
        context.resources.getString(R.string.graph_name_history),
        context.resources.getString(R.string.graph_name_year)
    )
    val builder = AlertDialog.Builder(context, R.style.dialog_with_rounded_corner)
        .setTitle(context.resources.getString(R.string.graphs_list_title))
        .setItems(a) { _, which ->
            when (which) {
                0 -> {
                    BikeCounterActivity.startActivity(context, data[position].name)
                    context.setScrollingAnimationRightToLeft()
                }
                1 -> {
                    YearCounterActivity.startActivity(context, data[position].name)
                    context.setScrollingAnimationRightToLeft()
                }
                else -> Log.e("test output", "Unknown choice selected")
            }
        }
    val dialog = builder.create()
    dialog.window?.attributes?.gravity = gravity
    dialog.window?.attributes?.x = x
    dialog.window?.attributes?.y = y

    dialog.show()
    dialog.window?.setLayout(500, dialog.window?.attributes?.height ?: 600)

    return true
}
