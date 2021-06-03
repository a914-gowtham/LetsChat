package com.gowtham.letschat.views

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.os.BuildCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputConnectionCompat.OnCommitContentListener
import androidx.core.view.inputmethod.InputContentInfoCompat


class CustomEditText : AppCompatEditText {
    lateinit var imgTypeString: Array<String>
    private var keyBoardInputCallbackListener: KeyBoardInputCallbackListener? = null

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) :
            super(context, attrs) {
        initView()
    }

    private fun initView() {
        imgTypeString = arrayOf(
            "image/png",
            "image/gif",
            "image/jpeg",
            "image/webp"
        )
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        val ic: InputConnection = super.onCreateInputConnection(outAttrs)!!
        EditorInfoCompat.setContentMimeTypes(
            outAttrs!!,
            imgTypeString
        )
        return InputConnectionCompat.createWrapper(ic, outAttrs, callback)
    }

    val callback =
        OnCommitContentListener { inputContentInfo, flags, opts ->
            // read and display inputContentInfo asynchronously
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && flags and
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    return@OnCommitContentListener false // return false if failed
                }
            }
            var supported = false
            for (mimeType in imgTypeString) {
                if (inputContentInfo.description.hasMimeType(mimeType)) {
                    supported = true
                    break
                }
            }
            if (!supported)
                return@OnCommitContentListener false
            keyBoardInputCallbackListener?.onCommitContent(inputContentInfo, flags, opts)
            true // return true if succeeded
        }

    interface KeyBoardInputCallbackListener {
        fun onCommitContent(
            inputContentInfo: InputContentInfoCompat?,
            flags: Int, opts: Bundle?
        )
    }

    fun setKeyBoardInputCallbackListener(keyBoardInputCallbackListener: KeyBoardInputCallbackListener?) {
        this.keyBoardInputCallbackListener = keyBoardInputCallbackListener
    }
}