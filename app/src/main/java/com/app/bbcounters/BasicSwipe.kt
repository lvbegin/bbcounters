package com.app.bbcounters

import android.view.MotionEvent
import android.view.View

class BasicSwipe : View.OnTouchListener {
    private var onDown : Pair<Float, Float>? = null
    private var lastBeforeUp : Pair<Float, Float>? = null
    var action : (() -> Unit) = { }
    var condition: ((Pair<Float, Float>, Pair<Float, Float>) -> Boolean) = {_, _ -> false }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        if (p1 == null)
            return p0?.onTouchEvent(null) ?: false
        when(p1.action) {
            MotionEvent.ACTION_DOWN -> onDown = Pair(p1.x, p1.y)
            MotionEvent.ACTION_MOVE -> lastBeforeUp = Pair(p1.x, p1.y)
            MotionEvent.ACTION_UP -> {
                if (condition((onDown ?: Pair(0f,0f)), (lastBeforeUp ?: Pair(0f,0f))))
                    action()
            }
        }
        return p0?.onTouchEvent(p1) ?: true
    }
}