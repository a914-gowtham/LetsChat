package com.gowtham.letschat.core

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.gowtham.letschat.FirebasePush
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.di.MessageCollection
import com.gowtham.letschat.fragments.single_chat.toDataClass
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
    @MessageCollection
    private val messageCollection: CollectionReference,
    private val preference: MPreference){

    private val messagesList: MutableList<Message> by lazy { mutableListOf() }

    private var fromUser=preference.getUid()

    val message= MutableLiveData<String>()

    private lateinit var chatUsers: List<ChatUser>

    private val listOfDoc=ArrayList<String>()

    private lateinit var messageCollectionGroup: Query

    companion object{

        private var listenerDoc1: ListenerRegistration?=null
        private var listenerDoc2: ListenerRegistration?=null
        private var instanceCreated=false

        fun removeListeners(){
            instanceCreated=false
            listenerDoc1?.remove()
            listenerDoc2?.remove()
        }
    }

    fun initHandler(){
            if (instanceCreated)
                return
            else
                instanceCreated=true
             fromUser= preference.getUid()
            Timber.v("ChatHandler init")
            messageCollectionGroup=UserUtils.getMessageSubCollectionRef()
            preference.clearCurrentUser()

            listenerDoc1= messageCollectionGroup.whereArrayContains("chatUsers", fromUser!!)
                .addSnapshotListener { snapShots, error ->
                    if (error==null){
                        messagesList.clear()
                        listOfDoc.clear()
                        val listOfIds=ArrayList<String>()
                        snapShots?.forEach { doc->
                            val parentDoc=doc.reference.parent.parent?.id!!
                            val message= doc.data.toDataClass<Message>()
                            message.chatUserId =if (message.from != fromUser) message.from else message.to
                                if (isNotOnlineUser(message)){
                                    messagesList.add(message)
                                    if (!listOfDoc.contains(parentDoc)){
                                        listOfDoc.add(doc.reference.parent.parent?.id.toString())
                                        listOfIds.add(message.chatUserId!!)
                                    }}
                                else
                                    Timber.i("Online User ${preference.getOnlineUser()}")
                        }
                        updateChatUserIdInMessage(listOfIds)
                    }else
                        Timber.v(error)
                }
    }

    private fun updateChatUserIdInMessage(listOfIds : ArrayList<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            if (!messagesList.isNullOrEmpty()) {
                val list=ArrayList<ChatUser>()
                val unSavedUsersId=ArrayList<String>()  //message from new user not saved in localdb yet
                val locallySaved=ArrayList<String>()    //already saved in local db,not saved to mobile contacts
                chatUsers=dbRepository.getChatUserList()
                for ((index, doc) in listOfDoc.withIndex()) {
                    val chatUser = chatUsers.firstOrNull() { it.id == listOfIds[index] }
                    if (chatUser==null){
                        unSavedUsersId.add(listOfIds[index])
                        //message from unsaved user
                    }else{
                        if (!chatUser.locallySaved)
                            locallySaved.add(chatUser.id)
                        chatUser.unRead=messagesList.getUnreadCount(chatUser.id)
                        chatUser.documentId =doc
                        Timber.v("UserId ${chatUser.id}  count ${chatUser.unRead}")
                        list.add(chatUser)
                    }
                }
                dbRepository.insertMultipleMessage(messagesList)
                dbRepository.insertMultipleUser(list)
                delay(500)
                updateOnDb(list,unSavedUsersId,locallySaved)
            }
        }
    }

    private fun isNotOnlineUser(message: Message): Boolean {
        return preference.getOnlineUser()
            .isEmpty() || preference.getOnlineUser() != message.chatUserId
    }

    private fun updateOnDb(list: ArrayList<ChatUser>,
                           unSavedUsersId: ArrayList<String>,
                           locallySaved: ArrayList<String>) {
             showNotification(unSavedUsersId,locallySaved)
             val statusUpdater= MessageStatusUpdater(messageCollection)
             statusUpdater.updateToDelivery(fromUser!!,messagesList,*chatUsers.toTypedArray())
    }

    private fun showNotification(
        unSavedUsersId: ArrayList<String>,
        locallySaved: ArrayList<String>) {

        if (unSavedUsersId.isEmpty() && locallySaved.isEmpty()) {
//            Utils.removeNotification(context)
               val lastMsgId= messagesList.maxOf { it.createdAt }
               val msg=messagesList.find { it.createdAt==lastMsgId }
             if (msg!=null && msg.from !=fromUser)
                FirebasePush.showNotification(context, dbRepository)
        } else {
            //unsaved new user
            for (userId in unSavedUsersId) {
                val util = ChatUserUtil(dbRepository, usersCollection, null)
                val unreadCount = messagesList.getUnreadCount(userId)
                util.queryNewUserProfile(
                    context,
                    userId,
                    listOfDoc.firstOrNull { it.contains(userId) },unreadCount)
            }
            //unsaved in mobile contacts and already saved in local
            for (userId in locallySaved) {
                val unreadCount = messagesList.getUnreadCount(userId)
                val util = ChatUserUtil(dbRepository, usersCollection, null)
                util.queryNewUserProfile(
                    context,
                    userId,
                    listOfDoc.firstOrNull { it.contains(userId) },
                    unreadCount
                )
            }
        }
    }
}