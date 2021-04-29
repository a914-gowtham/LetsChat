package com.gowtham.letschat.core

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.gowtham.letschat.FirebasePush
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.di.MessageCollection
import com.gowtham.letschat.fragments.single_chat.toDataClass
import com.gowtham.letschat.utils.LogMessage
import com.gowtham.letschat.utils.MPreference
import com.gowtham.letschat.utils.UserUtils
import com.gowtham.letschat.utils.getUnreadCount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dbRepository: DbRepository,
    private val usersCollection: CollectionReference,
    private val preference: MPreference,
    private val messageStatusUpdater: MessageStatusUpdater
) {

    private val messagesList: MutableList<Message> by lazy { mutableListOf() }

    private var fromUser = preference.getUid()

    val message = MutableLiveData<String>()

    private lateinit var chatUsers: List<ChatUser>

    private val listOfDocs = ArrayList<String>()

    private lateinit var messageCollectionGroup: Query

    private val chatUserUtil = ChatUserUtil(dbRepository, usersCollection, null)

    companion object {

        private var listenerDoc1: ListenerRegistration? = null
        private var instanceCreated = false
        var isSingleChatOpen = true

        fun removeListeners() {
            instanceCreated = false
            listenerDoc1?.remove()
        }
    }

    fun initHandler() {
        if (instanceCreated)
            return
        instanceCreated = true
        fromUser = preference.getUid()
        Timber.v("ChatHandler init")
        messageCollectionGroup = UserUtils.getMessageSubCollectionRef()
        preference.clearCurrentUser()

    }

    private fun insertMessageOnDb(listOfIds: ArrayList<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            val contacts = ArrayList<ChatUser>()
            val newContactIds =
                ArrayList<String>()  //message from new user not saved in localdb yet
            chatUsers = dbRepository.getChatUserList()
            dbRepository.insertMultipleMessage(messagesList)
            for ((index, doc) in listOfDocs.withIndex()) {
                val chatUser = chatUsers.firstOrNull { it.id == listOfIds[index] }
                if (chatUser == null) {
                    newContactIds.add(listOfIds[index])
                    //message from unsaved user
                } else {
                    chatUser.unRead = if (preference.getOnlineUser() == chatUser.id) 0 else
                        dbRepository.getChatsOfFriend(chatUser.id).getUnreadCount(chatUser.id)
                    chatUser.documentId = doc
                    Timber.v("UserId ${chatUser.id}  count ${chatUser.unRead}")
                    contacts.add(chatUser)
                }
            }
            dbRepository.insertMultipleUsers(contacts)
            val currentChatUser = if (preference.getOnlineUser().isNotEmpty())
                contacts.firstOrNull { it.id==preference.getOnlineUser() }
            else null
            withContext(Dispatchers.Main) {
                updateMsgStatus(newContactIds, currentChatUser)
            }
        }

    }

    private fun updateMsgStatus(
        newContactIds: ArrayList<String>,
        currentChatUser: ChatUser?
    ) {
        showNotification(newContactIds)
        if (isSingleChatOpen && currentChatUser != null) {
            val currentUserMsgs = messagesList.filter {
                it.chatUserId == currentChatUser.id
            }
            val otherUserMsgs = messagesList.filter {
                it.chatUserId != currentChatUser.id
            }
            messageStatusUpdater.updateToDelivery(otherUserMsgs, *chatUsers.toTypedArray())
            messageStatusUpdater.updateToSeen(
                currentChatUser.id, currentChatUser.documentId!!, currentUserMsgs
            )
        } else {
            messageStatusUpdater.updateToDelivery(messagesList, *chatUsers.toTypedArray())
        }
    }

    private fun showNotification(
        newContactIds: ArrayList<String>
    ) {
        if (newContactIds.isEmpty()) {
            val lastMsgId = messagesList.maxOf { it.createdAt }
            val msg = messagesList.find { it.createdAt == lastMsgId }
            if (msg != null && msg.from != fromUser)
                FirebasePush.showNotification(context, dbRepository)
        } else {
            //unsaved new user
            for (i in 0 until newContactIds.size) {
                val userId = newContactIds[i]
                if (userId == preference.getOnlineUser())
                    continue

                val unreadCount = messagesList.getUnreadCount(userId)
                chatUserUtil.queryNewUserProfile(
                    context,
                    userId,
                    listOfDocs.firstOrNull { it.contains(userId) }, unreadCount,
                    showNotification = i == newContactIds.lastIndex
                )
            }
        }
    }
}