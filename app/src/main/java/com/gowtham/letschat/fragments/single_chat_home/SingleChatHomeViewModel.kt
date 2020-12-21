package com.gowtham.letschat.fragments.single_chat_home

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.DefaultDbRepo
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.utils.LogMessage
import com.gowtham.letschat.utils.MPreference

class SingleChatHomeViewModel  @ViewModelInject
constructor(private val dbRepo: DefaultDbRepo): ViewModel() {

    val message= MutableLiveData<String>()

    init {
//        LogMessage.v("SingleChatHomeVModel init")
    }

    fun getChatUsers() = dbRepo.getChatUserWithMessages()

    fun getChatUsersAsList() = dbRepo.getChatUserWithMessagesList()

    override fun onCleared() {
//        LogMessage.v("SingleChatHOME cleared")
        super.onCleared()
    }

    fun insertChatUser(chatUser: ChatUser) = dbRepo.insertUser(chatUser)

    fun insertMultipleChatUser(users : List<ChatUser>) = dbRepo.insertMultipleUser(users)

    fun getAllChatUser() = dbRepo.getAllChatUser()

    fun deleteUser(userId: String) = dbRepo.deleteUserById(userId)

}