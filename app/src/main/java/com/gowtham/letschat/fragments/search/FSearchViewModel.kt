package com.gowtham.letschat.fragments.search

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gowtham.letschat.utils.Constants
import com.gowtham.letschat.utils.DataStorePreference
import com.gowtham.letschat.utils.LoadState
import com.gowtham.letschat.utils.LogMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class FSearchViewModel @Inject constructor(repository: SearchRepo,
                                               private val dataStorePreference: DataStorePreference): ViewModel() {

    private val searchHandler = Handler(Looper.getMainLooper())

    private var lastQuery=""

    private var currentQuery=MutableLiveData<String>()

    private var _loadState=MutableLiveData<LoadState>()

    val loadState get() = _loadState

    init {
        LogMessage.v("FSearchViewModel")
    }
/*
    val users= Transformations.switchMap(currentQuery){ query->
        callMe(query)
    }

    private fun callMe(query: String?): LiveData<Any> {
        return users
    }*/

    fun getCachedList() = dataStorePreference.getList()

    fun makeQuery(query: String){
        if(lastQuery==query)
            return
        lastQuery=query
        removeTypingCallbacks()
        searchHandler.postDelayed(queryThread, 400)
    }

    fun setLoadState(state: LoadState){
        _loadState.value=state
    }

    fun getLoadState(): LiveData<LoadState>{
        return _loadState
    }

    private val queryThread = Runnable {
        currentQuery.value=lastQuery
        repository.makeQuery(lastQuery,_loadState)
        removeTypingCallbacks()
    }



    private fun removeTypingCallbacks() {
        searchHandler.removeCallbacks(queryThread)
    }

    fun clearCachedUser() {
        dataStorePreference.storeList(Constants.KEY_LAST_QUERIED_LIST, emptyList())
    }

}