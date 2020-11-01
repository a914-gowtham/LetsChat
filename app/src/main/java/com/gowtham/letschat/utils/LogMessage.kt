package com.gowtham.letschat.utils

import android.util.Log

object LogMessage {

    private const val logVisible = true

    internal fun v(msg: String) {
        if (logVisible) Log.v("LetsChat",msg)
    }

    internal fun e(msg: String) {
        if (logVisible) Log.e("LetsChat",msg)
    }

}