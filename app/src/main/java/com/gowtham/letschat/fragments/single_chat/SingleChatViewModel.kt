package com.gowtham.letschat.fragments.single_chat

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.firebase.database.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.reflect.TypeToken
import com.gowtham.letschat.TYPE_NEW_MESSAGE
import com.gowtham.letschat.core.MessageSender
import com.gowtham.letschat.core.MessageStatusUpdater
import com.gowtham.letschat.core.OnMessageResponse
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.di.MessageCollection
import com.gowtham.letschat.models.UserStatus
import com.gowtham.letschat.services.UploadWorker
import com.gowtham.letschat.utils.Constants.CHAT_USER_DATA
import com.gowtham.letschat.utils.Constants.MESSAGE_DATA
import com.gowtham.letschat.utils.Constants.MESSAGE_FILE_URI
import com.gowtham.letschat.utils.LogMessage
import com.gowtham.letschat.utils.MPreference
import com.gowtham.letschat.utils.UserUtils
import com.gowtham.letschat.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.reflect.full.memberProperties

@HiltViewModel
class SingleChatViewModel @Inject
constructor(
    @ApplicationContext private val context: Context,
    private val dbRepository: DbRepository,
    @MessageCollection
    private val messageCollection: CollectionReference,
    private val preference: MPreference,
    private val firebaseFireStore: FirebaseFirestore,
    ) : ViewModel() {

    private val database = FirebaseDatabase.getInstance()

    private val toUser = preference.getOnlineUser()

    private val fromUser = preference.getUid()

    val message = MutableLiveData<String>()

    private val statusRef: DatabaseReference = database.getReference("Users/$toUser")

    private var statusListener: ValueEventListener? = null

    val chatUserOnlineStatus = MutableLiveData(UserStatus())

    private val messageStatusUpdater=MessageStatusUpdater(messageCollection,firebaseFireStore)

    private lateinit var chatUser: ChatUser

    private val typingHandler = Handler(Looper.getMainLooper())

    private var isTyping = false

    private var canScroll = false

    private var chatUserOnline = false

    init {
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

    fun setChatUser(chatUser: ChatUser) {
        if (!this::chatUser.isInitialized) {
            this.chatUser = chatUser
            setSeenAllMessage()
        }
    }

    fun canScroll(can: Boolean) {
        canScroll = can
    }

    fun getCanScroll() = canScroll

    fun getMessagesByChatUserId(chatUserId: String) =
        dbRepository.getMessagesByChatUserId(chatUserId)

    fun sendMessage(message: Message) {
        Handler(Looper.getMainLooper()).postDelayed({
            val messageSender = MessageSender(
                messageCollection,
                dbRepository,
                chatUser,
                messageListener
            )
            messageSender.checkAndSend(fromUser!!, toUser, message)
        }, 400)
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
            val nonSendMsgs =
                listOfMessage.filter { it.from == fromUser && it.status == 0 && it.type == "text" }
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
            if (chatUser.user.token.isNotEmpty())
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
        statusListener?.let {
            statusRef.removeEventListener(it)
        }
        super.onCleared()
    }

    fun setSeenAllMessage() {
        LogMessage.v("SetSeenAllMessage called")
        if (this::chatUser.isInitialized) {
            chatUser.unRead = 0
            dbRepository.insertUser(chatUser)
            viewModelScope.launch(Dispatchers.IO) {
                val messageList = dbRepository.getChatsOfFriend(chatUser.id)
                withContext(Dispatchers.Main){
                    if(messageList.isNotEmpty())
                      updateToSeen(messageList)
                }
            }
        }
    }

    private fun updateToSeen(messageList: List<Message>) {
        chatUser.documentId?.let {
            messageStatusUpdater.updateToSeen(toUser, it, messageList)
        }
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

    fun setUnReadCountZero(chatUser: ChatUser) {
        UserUtils.setUnReadCountZero(dbRepository, chatUser)
    }

    fun insertUser(chatUser: ChatUser) {
        dbRepository.insertUser(chatUser)
    }

    fun uploadToCloud(message: Message, fileUri: String) {
        try {
            dbRepository.insertMessage(message)
            removeTypingCallbacks()
            val messageData = Json.encodeToString(message)
            val chatUserData = Json.encodeToString(chatUser)
            val data = Data.Builder()
                .putString(MESSAGE_FILE_URI, fileUri)
                .putString(MESSAGE_DATA, messageData)
                .putString(CHAT_USER_DATA, chatUserData)
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

inline fun <reified T : Any> T.asMap(): Map<String, Any?> {
    val props = T::class.memberProperties.associateBy { it.name }
    return props.keys.associateWith { props[it]?.get(this) }
}