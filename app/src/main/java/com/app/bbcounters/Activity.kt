package com.app.bbcounters

import android.app.Activity

fun Activity.setScrollingAnimationRightToLeft() =
    this.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

fun Activity.setScrollingAnimationLeftToRight() =
    this.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_rigth)

fun Activity.finishWithScrollingLeftToRight() {
    this.finish()
    this.setScrollingAnimationLeftToRight()
}