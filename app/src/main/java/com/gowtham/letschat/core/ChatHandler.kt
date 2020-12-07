package com.gowtham.letschat.core

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.gowtham.letschat.FirebasePush
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.di.MessageCollection
import com.gowtham.letschat.fragments.single_chat.toDataClass
import com.gowtham.letschat.utils.MPreference
import com.gowtham.letschat.utils.UserUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            CoroutineScope(Dispatchers.IO).launch{
                chatUsers=dbRepository.getChatUserList()
            }

            listenerDoc1= messageCollectionGroup.whereEqualTo("from",fromUser)
                .addSnapshotListener { snapShots, error ->
                    if (error==null){
                        messagesList.clear()
                        listOfDoc.clear()
                        val listOfIds=ArrayList<String>()
                        snapShots?.forEach { doc->
                            val parentDoc=doc.reference.parent.parent?.id!!
                                val message= doc.data.toDataClass<Message>()
                            Timber.v("CreatedAt1 ${message.createdAt}")
                            message.chatUserId =if (message.from != fromUser) message.from else message.to
                                if (isNotOnlineUser(message)){
                                    messagesList.add(message)
                                    if (!listOfDoc.contains(parentDoc)){
                                        listOfDoc.add(doc.reference.parent.parent?.id.toString())
                                        listOfIds.add(message.chatUserId!!)
                                    }}
                                else
                                    Timber.i("Online User ${preference.getOnlineUser()}")
                            Timber.v("Message List size ${messagesList.size}")
                        }
                        updateChatUserIdInMessage(listOfIds)
                    }else
                        Timber.v(error)
                }

            listenerDoc2= messageCollectionGroup.whereEqualTo("to",fromUser)
                .addSnapshotListener { snapShots, error ->
                    if (error==null){
                        val listOfIds=ArrayList<String>()
                        snapShots?.forEach { doc->
                            val parentDoc=doc.reference.parent.parent?.id!!
                                val message= doc.data.toDataClass<Message>()
                            Timber.v("CreatedAt2 ${message.createdAt}")
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
                        chatUser.unRead=messagesList.filter { it.from==chatUser.id &&
                                it.status<3 }.size
                        chatUser.documentId =doc
                        list.add(chatUser)
                    }
                }
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
        val statusUpdater= MessageStatusUpdater(messageCollection)
        statusUpdater.updateToDelivery(fromUser!!,messagesList,*chatUsers.toTypedArray())
        Timber.v("My message ${messagesList.size}")
        CoroutineScope(Dispatchers.IO).launch {
            dbRepository.insertMultipleMessage(messagesList)
            dbRepository.insertMultipleUser(list)
            val lastMessage=dbRepository.getMessageList()
            withContext(Dispatchers.Main) {
                showNotification(unSavedUsersId,locallySaved,lastMessage)
            }
        }
    }

    private fun showNotification(
        unSavedUsersId: ArrayList<String>,
        locallySaved: ArrayList<String>,
        lastMessage: List<Message>) {
        /*   val ignoreNoti=(isFirstTime && unSavedUsersId.isEmpty())
           if (ignoreNoti) {
               isFirstTime=false
               return
           }
   */

        if (unSavedUsersId.isEmpty() && locallySaved.isEmpty()) {
//            Utils.removeNotification(context)
            FirebasePush.showNotification(context, dbRepository)
        } else {
            //unsaved new user
            for (userId in unSavedUsersId) {
                val util = ChatUserUtil(dbRepository, usersCollection, null)
                util.queryNewUserProfile(
                    context,
                    userId,
                    listOfDoc.firstOrNull { it.contains(userId) })
            }
            //unsaved in mobile contacts and already saved in local
            for (userId in locallySaved) {
                val unreadCount = messagesList.filter {
                    it.from == it.chatUserId &&
                            it.status < 3
                }.size
                val util = ChatUserUtil(dbRepository, usersCollection, null)
                util.queryNewUserProfile(
                    context,
                    userId,
                    listOfDoc.firstOrNull { it.contains(userId) },
                    unreadCount
                )
            }
        }
        dbRepository.insertMultipleMessage(messagesList)
    }
}