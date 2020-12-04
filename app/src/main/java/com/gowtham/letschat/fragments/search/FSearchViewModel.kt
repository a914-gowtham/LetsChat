package com.gowtham.letschat.fragments.search

import android.os.Handler
import android.os.Looper
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.gowtham.letschat.utils.LogMessage

class FSearchViewModel @ViewModelInject constructor(repository: SearchRepo): ViewModel() {

    private val searchHandler = Handler(Looper.getMainLooper())

    private var lastQuery=""

    private var currentQuery=MutableLiveData<String>()

    val users=currentQuery.switchMap { query->
         callMe(query)
    }

    private fun callMe(query: String?): LiveData<Any> {
        return users
    }

    init {
        LogMessage.v("FSearchViewModel")
    }

    fun makeQuery(query: String){
        if(lastQuery==query)
            return
        lastQuery=query
        removeTypingCallbacks()
        searchHandler.postDelayed(queryThread, 400)
    }

    private val queryThread = Runnable {
        currentQuery.value=lastQuery
        removeTypingCallbacks()
    }

    private fun removeTypingCallbacks() {
        searchHandler.removeCallbacks(queryThread)
    }

}