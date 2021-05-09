package com.gowtham.letschat.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.CallSuper
import androidx.core.app.RemoteInput
import com.google.firebase.firestore.CollectionReference
import com.gowtham.letschat.KEY_TEXT_REPLY
import com.gowtham.letschat.TYPE_NEW_MESSAGE
import com.gowtham.letschat.core.MessageSender
import com.gowtham.letschat.core.MessageStatusUpdater
import com.gowtham.letschat.core.OnMessageResponse
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.db.data.ChatUserWithMessages
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.db.data.TextMessage
import com.gowtham.letschat.di.MessageCollection
import com.gowtham.letschat.utils.Constants.ACTION_MARK_AS_READ
import com.gowtham.letschat.utils.Constants.ACTION_REPLY
import com.gowtham.letschat.utils.Constants.CHAT_DATA
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

abstract class HiltBroadcastReceiver : BroadcastReceiver(){
    @CallSuper
    override fun onReceive(context: Context?, intent: Intent?) { }
}

@AndroidEntryPoint
class NActionReceiver : HiltBroadcastReceiver(), OnMessageResponse {

    @Inject
    lateinit var preference: MPreference

    @Inject
    lateinit var messageDao: MessageDao

    @Inject
    lateinit var messageStatusUpdater: MessageStatusUpdater

    @Inject
    lateinit var dbRepo: DbRepository

    @MessageCollection
    @Inject
    lateinit var messageCollection: CollectionReference

    private var notificationId: Int=0

    lateinit var context: Context

    lateinit var chatUser: ChatUserWithMessages

    private lateinit var myUserId: String

    private lateinit var chatUserId: String

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        try {
            this.context=context!!
            myUserId = preference.getUid()!!
            chatUser = intent!!.getParcelableExtra(CHAT_DATA)!!
            val notiIdString = chatUser.user.user.createdAt.toString()
            //last 4 digits as notificationId
            notificationId= notiIdString.substring(notiIdString.length - 4)
                .toInt()

            chatUserId = UserUtils.getChatUserId(myUserId, chatUser.messages.last())
            if (intent.action == ACTION_MARK_AS_READ) {
                chatUser.messages.let {
                    messageStatusUpdater.updateToSeen(
                        chatUserId,chatUser.user.documentId!!,it)
                }
                Utils.removeNotificationById(context, notificationId)
                updateOnDb()
            } else if (intent.action == ACTION_REPLY) {
                val reply = getMessageText(intent)
                if (reply.isNotBlank()) {
                    val message = createMessage(reply, myUserId, chatUserId)
                    message.chatUserId=chatUserId
                    val messageSender = MessageSender(messageCollection,
                        dbRepo,
                        chatUser.user,
                        this)
                    messageSender.checkAndSend(myUserId, chatUserId, message)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun updateOnDb() {
        val list= chatUser.messages.filter {  it.status<3 && it.to==myUserId }.map {
            it.status=3
            it
        }
        //seen message other message of this user
        chatUser.user.unRead=0
        dbRepo.insertUser(chatUser.user)
        CoroutineScope(Dispatchers.IO).launch {
            dbRepo.insertMultipleMessage(list.toMutableList())
        }
    }

    private fun createMessage(reply: String, myUserId: String,
                              chatUserId: String): Message {
        val profile = preference.getUserProfile()!!
        val txtMessage = TextMessage(reply)
        return Message(
            System.currentTimeMillis(),
            from = myUserId,
            to = chatUserId,
            senderName = profile.userName,
            senderImage = profile.image,
            textMessage = txtMessage,
            status = 0
        )
    }

    private fun getMessageText(intent: Intent): String {
        return RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT_REPLY).toString()
    }

    override fun onSuccess(message: Message) {
        Utils.removeNotificationById(context, notificationId)
        //update to seen status
        chatUser.messages.let {
            messageStatusUpdater.updateToSeen(
                chatUserId,chatUser.user.documentId!!,it)
        }
        updateOnDb()
        val token=chatUser.user.user.token
        if (token.isNotBlank())
            UserUtils.sendPush(context, TYPE_NEW_MESSAGE,Json.encodeToString(message),token,message.to)
        dbRepo.insertMessage(message)
    }

    override fun onFailed(message: Message) {
        dbRepo.insertMessage(message)
    }
}