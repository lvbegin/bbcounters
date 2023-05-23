package com.app.bbcounters

import android.util.Log

class Log {
    companion object {
        private val logEnabled = BuildConfig.DEBUG

        fun i(tag : String, message : String)  {
            if (logEnabled)
                Log.i(tag, message)
        }

        fun e(tag : String, message : String) {
            if (logEnabled)
                Log.e(tag, message)
        }

        fun d(tag : String, message : String) {
            if (logEnabled)
                Log.d(tag, message)
        }

        fun v(tag : String, message : String) {
            if (logEnabled)
                Log.v(tag, message)
        }

        fun w(tag : String, message: String) {
            if (logEnabled)
                Log.w(tag, message)
        }
    }
}