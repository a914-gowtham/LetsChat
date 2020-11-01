package com.gowtham.letschat.ui.activities

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.models.Country
import com.gowtham.letschat.utils.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import java.util.*
import javax.inject.Singleton
import kotlin.concurrent.schedule

@ActivityScoped
@Singleton
class SharedViewModel @ViewModelInject
constructor(@ApplicationContext private val context: Context,
            private val userDao: ChatUserDao,
            private val preference: MPreference) : ViewModel() {

    val country = MutableLiveData<Country>()

    val openMainAct = MutableLiveData<Boolean>()

    private var timer: TimerTask? = null

    init {
        "Init SharedViewModel".printMeD()
    }

    fun setCountry(country: Country) {
        this.country.value = country
    }

    fun onFromSplash() {
        if (timer == null) {
            timer = Timer().schedule(2000) {
              openMainAct.postValue(true)
            }
        }
    }

    fun getChats() = userDao.getChatUserWithMessages()

    override fun onCleared() {
        super.onCleared()
        "onCleared SharedViewModel".printMeD()
    }
}