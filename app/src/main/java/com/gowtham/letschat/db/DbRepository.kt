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
    @ApplicationContext private val context: Context,
    private val userDao: ChatUserDao,
    private val preference: MPreference,
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

    fun insertGroup(group: Group) {
        CoroutineScope(Dispatchers.IO).launch {
            groupDao.insertGroup(group)
        }
    }

    fun insertMultipleMessage(messagesList: MutableList<Message>) {
        CoroutineScope(Dispatchers.IO).launch {
            messageDao.insertMultipleMessage(messagesList)
        }
    }

    fun insertMultipleGroupMessage(messagesList: MutableList<GroupMessage>) {
        CoroutineScope(Dispatchers.IO).launch {
            groupMsgDao.insertMultipleMessage(messagesList)
        }
    }


    fun insertMessage(message: Message) {
        CoroutineScope(Dispatchers.IO).launch {
            val size=messageDao.getMessageList().size
            LogMessage.e("Size before $size")
            messageDao.insertMessage(message)
            LogMessage.e("Size afte")
        }
    }

    fun insertMessage(message: GroupMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            groupMsgDao.insertMessage(message)
        }
    }

    fun insertMultipleGroup(groups: List<Group>) {
        CoroutineScope(Dispatchers.IO).launch {
            groupDao.insertMultipleGroup(groups)
        }
    }

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