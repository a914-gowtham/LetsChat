package com.gowtham.letschat.fragments.search

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import androidx.paging.cachedIn
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.utils.LogMessage
import com.gowtham.letschat.utils.UserUtils
import dagger.hilt.android.qualifiers.ApplicationContext

class FSearchViewModel @ViewModelInject constructor(repository: SearchRepo): ViewModel() {

    private val searchHandler = Handler(Looper.getMainLooper())

    private var lastQuery=""

    private var currentQuery=MutableLiveData<String>()

    val users=currentQuery.switchMap { query->
        repository.getSearchResults(query).cachedIn(viewModelScope)
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