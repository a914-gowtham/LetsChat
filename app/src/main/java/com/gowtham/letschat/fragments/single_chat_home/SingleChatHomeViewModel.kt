package com.gowtham.letschat.fragments.single_chat_home

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.di.MessageCollection
import com.gowtham.letschat.utils.LogMessage
import com.gowtham.letschat.utils.MPreference
import dagger.hilt.android.qualifiers.ApplicationContext

class SingleChatHomeViewModel  @ViewModelInject
constructor(@ApplicationContext private val context: Context,
            private val userDao: ChatUserDao,
            @MessageCollection
            private val messageCollection: CollectionReference,
            private val preference: MPreference): ViewModel() {

    private val messagesList: MutableList<Message> by lazy { mutableListOf() }

    val messagesMutableLiveData = MutableLiveData<List<Message>>()

    private val toUser=preference.getOnlineUser()

    private val fromUser=preference.getUid()

    val message= MutableLiveData<String>()

    private val configChanged= MutableLiveData(0)

    init {
        LogMessage.v("SingleChatHomeVModel init $toUser")
    }

    fun getChatUsers() = userDao.getChatUserWithMessages()

    fun getChatUsersw() = userDao.getChatUserWithMessagesList()

    override fun onCleared() {
        LogMessage.v("SingleChatHOME cleared")
        configChanged.value=0
        super.onCleared()
    }
}