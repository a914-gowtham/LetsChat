package com.gowtham.letschat.utils

import android.util.Log
import com.gowtham.letschat.BuildConfig.DEBUG

object LogMessage {

    private val logVisible = DEBUG

    internal fun v(msg: String) {
        if (logVisible) Log.v("LetsChat",msg)
    }

    internal fun e(msg: String) {
        if (logVisible) Log.e("LetsChat",msg)
    }

}