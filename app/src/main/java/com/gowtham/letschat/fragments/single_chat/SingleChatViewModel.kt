package com.gowtham.letschat.fragments.single_chat

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.firebase.database.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.reflect.TypeToken
import com.gowtham.letschat.TYPE_NEW_MESSAGE
import com.gowtham.letschat.core.MessageSender
import com.gowtham.letschat.core.MessageStatusUpdater
import com.gowtham.letschat.core.OnMessageResponse
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.di.MessageCollection
import com.gowtham.letschat.models.UserStatus
import com.gowtham.letschat.services.UploadWorker
import com.gowtham.letschat.utils.*
import com.gowtham.letschat.utils.Constants.CHAT_USER_DATA
import com.gowtham.letschat.utils.Constants.MESSAGE_DATA
import com.gowtham.letschat.utils.Constants.MESSAGE_FILE_URI
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import timber.log.Timber
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
class SingleChatViewModel @ViewModelInject
constructor(
    @ApplicationContext private val context: Context,
    private val dbRepository: DbRepository,
    @MessageCollection
    private val messageCollection: CollectionReference,
    private val preference: MPreference
) : ViewModel() {

    private val messagesList: MutableList<Message> by lazy { mutableListOf() }

    private val database = FirebaseDatabase.getInstance()

    private val toUser = preference.getOnlineUser()

    private val fromUser = preference.getUid()

    val message = MutableLiveData<String>()

    private var listenerDoc1: ListenerRegistration? = null

    private var listenerDoc2: ListenerRegistration? = null

    private val statusRef: DatabaseReference = database.getReference("Users/$toUser")

    private var statusListener: ValueEventListener? = null

    private var isOnlineStatus = true

    val chatUserOnlineStatus = MutableLiveData(UserStatus())

    private lateinit var chatUser: ChatUser

    private var chatsFromRoom = ArrayList<Message>()

    private val typingHandler = Handler(Looper.getMainLooper())

    private var isTyping = false

    private var canScroll = false

    private var statusUpdated = false

    private val doc1 = "${fromUser}_${toUser}"

    private val doc2 = "${toUser}_${fromUser}"

    private var firstCache = true

    private var cleared = false

    private var chatUserOnline = false

    private var mediaPlayer=MediaPlayer()

    init {
        LogMessage.v("SingleChatViewModel init Doc1 $doc1")
        LogMessage.v("SingleChatViewModel init Doc2 $doc2")

        listenerDoc1?.remove()
        listenerDoc1 = messageCollection.document(doc1)
            .collection("messages").addSnapshotListener { snapshot, error ->
                if (cleared)
                    return@addSnapshotListener
                val docs = snapshot?.documents
                LogMessage.v("Snapshot 1 ${snapshot?.metadata?.isFromCache!!}")
                if (snapshot.metadata.isFromCache) {
                    if (firstCache) {
                        callMe(doc1)
                        firstCache = false
                    }
                    return@addSnapshotListener
                }
                if (error == null) {
                    messagesList.clear()
                    if (docs.isNullOrEmpty())
                        return@addSnapshotListener
                    docs.forEach { doc ->
                        val message = doc.data?.toDataClass<Message>()
                        message?.chatUserId =
                            if (message?.from != fromUser) message?.from else message?.to
                        if (doc.id.toLong() > preference.getLogInTime())
                            messagesList.add(message!!)
                    }
                    if (!messagesList.isNullOrEmpty()) {
                        chatUser.documentId = doc1
                        Timber.v("Check state one")
                        dbRepository.insertMultipleMessage(messagesList)
                        dbRepository.insertUser(chatUser)
                        updateMessagesStatus();
                    }
                }
            }

        listenerDoc2?.remove()
        listenerDoc2 = messageCollection.document(doc2)
            .collection("messages").addSnapshotListener { snapshot, error ->
                if (cleared)
                    return@addSnapshotListener
                LogMessage.v("Snapshot 2 ${snapshot?.metadata?.isFromCache!!}")
                val docs = snapshot.documents
                if (snapshot.metadata.isFromCache) {
                    if (firstCache) {
                        callMe(doc2)
                        firstCache = false
                    }
                    return@addSnapshotListener
                }
                if (error == null) {
                    messagesList.clear()
                    if (docs.isNullOrEmpty()) {
                        return@addSnapshotListener
                    }
                    docs.forEach { doc ->
                        val message = doc.data?.toDataClass<Message>()
                        message?.chatUserId =
                            if (message?.from != fromUser) message?.from else message?.to
                        if (doc.id.toLong() > preference.getLogInTime())
                            messagesList.add(message!!)
                    }
                    if (!messagesList.isNullOrEmpty()) {
                        chatUser.documentId = doc2
                        Timber.v("Check state two")
                        dbRepository.insertMultipleMessage(messagesList)
                        dbRepository.insertUser(chatUser)
                        updateMessagesStatus();
                    }
                }
            }

        statusListener = statusRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userStatus = snapshot.getValue(UserStatus::class.java)
                chatUserOnlineStatus.value = userStatus
                chatUserOnline = userStatus?.status == "online"
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun callMe(doc: String) {
        messageCollection.document(doc)
            .collection("messages").get().addOnSuccessListener { snapshot ->
                val docs = snapshot.documents
                if (docs.isNotEmpty()) {
                    messagesList.clear()
                    docs.forEach { doc ->
                        val message = doc.data?.toDataClass<Message>()
                        message?.chatUserId =
                            if (message?.from != fromUser) message?.from else message?.to
                        if (doc.id.toLong() > preference.getLogInTime())
                            messagesList.add(message!!)
                    }

                    if (!messagesList.isNullOrEmpty()) {
                        chatUser.documentId = doc //
                        dbRepository.insertMultipleMessage(messagesList)
                        dbRepository.insertUser(chatUser)
                        updateMessagesStatus();
                    }
                }
            }
    }

    private fun updateMessagesStatus() {
        if (isOnlineStatus) {
            val updateToSeen = MessageStatusUpdater(messageCollection)
            updateToSeen.updateToSeen(fromUser!!, toUser, chatUser.documentId!!, messagesList)
        }
    }

    fun setChatUser(chatUser: ChatUser) {
        if (!this::chatUser.isInitialized)
            this.chatUser = chatUser
    }

    fun setChatsOfThisUser(list: MutableList<Message>) {
        chatsFromRoom = list as ArrayList<Message>
        /*   val filterList = chatsFromRoom
               .filter { it.status != 3 && it.from != fromUser }
               .map { it.status = 3
                   it
               }
           if (filterList.isNotEmpty()) {
               Timber.v("Seen updated locally")
               CoroutineScope(Dispatchers.IO).launch {
                   messageDao.insertMultipleMessage(filterList)
               }
           }*/
        if (!statusUpdated) {
            statusUpdated = true
            setSeenAllMessage()  //one time only
        }
    }

    fun canScroll(can: Boolean) {
        canScroll = can
    }

    fun getCanScroll() = canScroll

    private fun updateMessageStatus(message: Message) {
        if (isOnlineStatus) { //update message status to seen
            message.status = 3
            val seenTime = System.currentTimeMillis()
            message.deliveryTime = message.deliveryTime ?: seenTime
            message.seenTime = seenTime
        } else {    //update message status to delivered
            message.status = 2
            message.deliveryTime = System.currentTimeMillis()
        }
    }

    fun setOnline(online: Boolean) {
        isOnlineStatus = online
    }


/*    private fun updateMsgReceivedStatus(messagesList: List<Message>, document: String) {
        LogMessage.v("Message status update called")
        messageCollection.document(document).set(
            mapOf("messages" to messagesList),
        ).addOnSuccessListener {
            LogMessage.v("Message status updated successfully")
        }.addOnFailureListener {
            LogMessage.v("Message status update failed reason ${it.message}")
        }
    }*/

    fun getMessagesByChatUserId(chatUserId: String) =
        dbRepository.getMessagesByChatUserId(chatUserId)

    fun insertMultiMessage(list: MutableList<Message>){
        dbRepository.insertMultipleMessage(list)
    }

    fun sendMessage(message: Message) {
        Handler(Looper.getMainLooper()).postDelayed({
            val messageSender = MessageSender(
                messageCollection,
                dbRepository,
                chatUser,
                messageListener
            )
            messageSender.checkAndSend(fromUser!!, toUser, message)
        }, 300)
        dbRepository.insertMessage(message)
        removeTypingCallbacks()
    }

    fun sendCachedTxtMesssages() {
        //Send msg that is not sent succesfully in last time
        CoroutineScope(Dispatchers.IO).launch {
            updateCacheMessges(dbRepository.getChatsOfFriend(toUser))
        }
    }

    private suspend fun updateCacheMessges(listOfMessage: List<Message>) {
        withContext(Dispatchers.Main) {
            val nonSendMsgs = listOfMessage.filter { it.from == fromUser && it.status == 0 && it.type=="text"}
            LogMessage.v("nonSendMsgs Size ${nonSendMsgs.size}")
            if (nonSendMsgs.isNotEmpty()) {
                for (cachedMsg in nonSendMsgs) {
                    val messageSender = MessageSender(
                        messageCollection,
                        dbRepository,
                        chatUser,
                        messageListener
                    )
                    messageSender.checkAndSend(fromUser!!, toUser, cachedMsg)
                }
            }
        }
    }

    private val messageListener = object : OnMessageResponse {
        override fun onSuccess(message: Message) {
            LogMessage.v("messageListener OnSuccess ${message.textMessage?.text}")
            dbRepository.insertMessage(message)
            if (!chatUser.user.token.isEmpty())
                UserUtils.sendPush(
                    context,
                    TYPE_NEW_MESSAGE,
                    Json.encodeToString(message),
                    chatUser.user.token,
                    message.to
                )
        }

        override fun onFailed(message: Message) {
            LogMessage.v("messageListener onFailed ${message.createdAt}")
            dbRepository.insertMessage(message)
        }
    }

    override fun onCleared() {
        LogMessage.v("SingleChat cleared")
        cleared = true
        listenerDoc1?.remove()
        listenerDoc2?.remove()
        statusListener?.let {
            statusRef.removeEventListener(it)
        }
        super.onCleared()
    }

    fun setSeenAllMessage() {
        LogMessage.v("SetSeenAllMessage called")
        if (!this::chatUser.isInitialized) {
//            getChatUser()
        } else if (chatUser.documentId.isNullOrEmpty())
            setStatusUpdatedMsgs()
        else if (!messagesList.isNullOrEmpty() && isOnlineStatus) {
            val updateToSeen = MessageStatusUpdater(messageCollection)
            updateToSeen.updateToSeen(fromUser!!, toUser, chatUser.documentId!!, messagesList)
        } else if (!chatsFromRoom.isNullOrEmpty() && isOnlineStatus) {
            val updateToSeen = MessageStatusUpdater(messageCollection)
            updateToSeen.updateToSeen(fromUser!!, toUser, chatUser.documentId!!, chatsFromRoom)
        }
        if (isOnlineStatus)
            UserUtils.setUnReadCountZero(dbRepository, chatUser)

    }

    fun sendTyping(edtValue: String) {
        if (edtValue.isEmpty()) {
            if (isTyping)
                UserUtils.sendTypingStatus(database, false, fromUser!!, toUser)
            isTyping = false
        } else if (!isTyping) {
            UserUtils.sendTypingStatus(database, true, fromUser!!, toUser)
            isTyping = true
            removeTypingCallbacks()
            typingHandler.postDelayed(typingThread, 4000)
        }
    }

    private val typingThread = Runnable {
        isTyping = false
        UserUtils.sendTypingStatus(database, false, fromUser!!, toUser)
        removeTypingCallbacks()
    }

    private fun removeTypingCallbacks() {
        typingHandler.removeCallbacks(typingThread)
    }

    private fun setStatusUpdatedMsgs() {
        try {
            messageCollection.document("${fromUser}_${toUser}").get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        chatUser.documentId = "${fromUser}_${toUser}"
                        dbRepository.insertUser(chatUser)
                        setSeenAllMessage()
                    } else {
                        messageCollection.document("${toUser}_${fromUser}").get()
                            .addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {
                                    chatUser.documentId = "${toUser}_${fromUser}"
                                    dbRepository.insertUser(chatUser)
                                    setSeenAllMessage()
                                }
                            }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setUnReadCountZero(chatUser: ChatUser) {
        UserUtils.setUnReadCountZero(dbRepository,chatUser)
    }

    fun insertUser(chatUser: ChatUser) {
        dbRepository.insertUser(chatUser)
    }

    fun uploadToCloud(message: Message,fileUri: String){
        try {
            dbRepository.insertMessage(message)
            removeTypingCallbacks()
            val messageData=Json.encodeToString(message)
            val chatUserData=Json.encodeToString(chatUser)
            val data= Data.Builder()
                .putString(MESSAGE_FILE_URI,fileUri)
                .putString(MESSAGE_DATA,messageData)
                .putString(CHAT_USER_DATA,chatUserData)
                .build()
            val uploadWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<UploadWorker>()
                    .setInputData(data)
                    .build()
            WorkManager.getInstance(context).enqueue(uploadWorkRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


//convert a data class to a map
fun <T> T.serializeToMap(): Map<String, Any> {
    return convert()
}

//convert a map to a data class
inline fun <reified T> Map<String, Any>.toDataClass(): T {
    return convert()
}

//convert an object of type I to type O
inline fun <I, reified O> I.convert(): O {
    val json = Utils.getGSONObj().toJson(this)
    return Utils.getGSONObj().fromJson(json, object : TypeToken<O>() {}.type)
}