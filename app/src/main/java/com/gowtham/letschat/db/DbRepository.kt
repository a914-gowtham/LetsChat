package com.gowtham.letschat.db

import android.content.Context
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.db.daos.GroupMessageDao
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.utils.LogMessage
import com.gowtham.letschat.utils.MPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DbRepository @Inject constructor(
    private val userDao: ChatUserDao,
    private val groupDao: GroupDao,
    private val groupMsgDao: GroupMessageDao,
    private val messageDao: MessageDao) : DefaultDbRepo {

    override fun insertUser(user: ChatUser) {
        CoroutineScope(Dispatchers.IO).launch {
            userDao.insertUser(user)
        }
    }

    override fun insertMultipleUser(users: List<ChatUser>) {
        CoroutineScope(Dispatchers.IO).launch {
            userDao.insertMultipleUser(users)
        }
    }

    override fun getChatUserWithMessages() = userDao.getChatUserWithMessages()

    override fun getChatUserList() = userDao.getChatUserList()

    override fun getChatUserWithMessagesList() = userDao.getChatUserWithMessagesList()

    override fun getChatUserById(id: String) = userDao.getChatUserById(id)

    override fun getAllChatUser() = userDao.getAllChatUser()

    override fun nukeTable() {
    }

    override fun deleteUserById(userId: String) {
    }

    fun insertMultipleUser(finalList: ArrayList<ChatUser>) {
        CoroutineScope(Dispatchers.IO).launch {
            userDao.insertMultipleUser(finalList)
        }
    }

    suspend fun insertMultipleUsers(users: ArrayList<ChatUser>){
       userDao.insertMultipleUser(users)
   }

    fun insertGroup(group: Group) {
        CoroutineScope(Dispatchers.IO).launch {
            groupDao.insertGroup(group)
        }
    }

    suspend fun insertMultipleMessage(messagesList: MutableList<Message>) =
            messageDao.insertMultipleMessage(messagesList)


    suspend fun insertMultipleGroupMessage(messagesList: List<GroupMessage>) =
            groupMsgDao.insertMultipleMessage(messagesList)

    fun getAllNonSeenMessage() =
        messageDao.getAllNotSeenMessages()

    fun insertMessage(message: Message) {
        CoroutineScope(Dispatchers.IO).launch {
            messageDao.insertMessage(message)
        }
    }

    fun insertMessage(message: GroupMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            groupMsgDao.insertMessage(message)
        }
    }

   suspend fun insertMultipleGroup(groups: List<Group>) =
            groupDao.insertMultipleGroup(groups)


    fun getGroupWithMessages() = groupDao.getGroupWithMessages()

    fun getMessagesByChatUserId(chatUserId: String) = messageDao.getMessagesByChatUserId(chatUserId)

    fun getChatsOfFriend(toUser: String) = messageDao.getChatsOfFriend(toUser)

    fun getGroupById(groupId: String) = groupDao.getGroupById(groupId)

    fun getChatsOfGroupList(groupId: String) = groupMsgDao.getChatsOfGroupList(groupId)

    fun getChatsOfGroup(groupId: String) = groupMsgDao.getChatsOfGroup(groupId)

    fun getGroupWithMessagesList() = groupDao.getGroupWithMessagesList()

    fun getMessageList() = messageDao.getMessageList()

    fun getGroupList() = groupDao.getGroupList()
}