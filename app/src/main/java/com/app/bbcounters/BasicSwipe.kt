package com.app.bbcounters

import android.view.MotionEvent
import android.view.View

class BasicSwipe() : View.OnTouchListener {
    private var onDonw : Pair<Float, Float>? = null
    private var lastBeforeUp : Pair<Float, Float>? = null
    var action : (() -> Unit)? = null
    var condition: ((Pair<Float, Float>, Pair<Float, Float>) -> Boolean)? = null

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        if (p1 == null)
            return p0?.onTouchEvent(p1) ?: false
        when(p1.action) {
            MotionEvent.ACTION_DOWN -> onDonw = Pair(p1.x, p1.y)
            MotionEvent.ACTION_MOVE -> lastBeforeUp = Pair(p1.x, p1.y)
            MotionEvent.ACTION_UP -> {
                val toDo = action ?: { }
                val check = condition ?: { _, _ -> false }
                if (check((onDonw ?: Pair(0f,0f)), (lastBeforeUp ?: Pair(0f,0f))))
                    toDo()
            }
        }
        return p0?.onTouchEvent(p1) ?: true
    }
}