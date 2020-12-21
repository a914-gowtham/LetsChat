package com.gowtham.letschat.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.ChatUserWithMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class DbRepositoryTest : DefaultDbRepo {

    private val users = mutableListOf<ChatUser>()

    private val chatUserList=MutableLiveData<List<ChatUser>>(users)

    override fun insertUser(user: ChatUser) {
        users.add(user)
        chatUserList.value=users
    }

    override fun insertMultipleUser(usersList: List<ChatUser>) {
        users.addAll(usersList)
        chatUserList.value=users
    }

    override fun getAllChatUser(): LiveData<List<ChatUser>> {
        return chatUserList
    }

    override fun getChatUserList(): List<ChatUser> {
          return users;
    }

    override fun getChatUserById(id: String): ChatUser? {
            return users.firstOrNull { it.id==id }
    }

    override fun deleteUserById(userId: String) {
          users.removeIf { it.id==userId }
          chatUserList.value=users
    }

    override fun getChatUserWithMessages(): Flow<List<ChatUserWithMessages>> {
        return emptyFlow()
    }

    override fun getChatUserWithMessagesList(): List<ChatUserWithMessages> {
       return emptyList()
    }

    override fun nukeTable() {

    }

}