package com.gowtham.letschat.views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.gowtham.letschat.R

class CustomProgressView constructor(context: Context) :
    Dialog(context) {
    init {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        this.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        setCancelable(false)
        this.setContentView(view)
    }
}