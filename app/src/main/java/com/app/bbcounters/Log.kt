package com.app.bbcounters

import android.util.Log

class Log {
    companion object {
        private val logEnabled = BuildConfig.DEBUG;

        public fun i(tag : String, message : String)  {
            if (logEnabled)
                Log.i(tag, message)
        }

        public fun e(tag : String, message : String) {
            if (logEnabled)
                Log.e(tag, message)
        }

        public fun d(tag : String, message : String) {
            if (logEnabled)
                Log.d(tag, message)
        }

        public fun v(tag : String, message : String) {
            if (logEnabled)
                Log.v(tag, message)
        }

        public fun w(tag : String, message: String) {
            if (logEnabled)
                Log.w(tag, message)
        }
    }
}