package com.gowtham.letschat

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gowtham.letschat.core.ChatUserUtil
import com.gowtham.letschat.core.GroupMsgStatusUpdater
import com.gowtham.letschat.core.GroupQuery
import com.gowtham.letschat.core.MessageStatusUpdater
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.db.daos.GroupMessageDao
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.db.data.*
import com.gowtham.letschat.di.GroupCollection
import com.gowtham.letschat.di.MessageCollection
import com.gowtham.letschat.models.PushMsg
import com.gowtham.letschat.ui.activities.MainActivity
import com.gowtham.letschat.utils.*
import com.gowtham.letschat.utils.Constants.ACTION_GROUP_NEW_MESSAGE
import com.gowtham.letschat.utils.Constants.ACTION_LOGGED_IN_ANOTHER_DEVICE
import com.gowtham.letschat.utils.Constants.ACTION_MARK_AS_READ
import com.gowtham.letschat.utils.Constants.ACTION_NEW_MESSAGE
import com.gowtham.letschat.utils.Constants.ACTION_REPLY
import com.gowtham.letschat.utils.Constants.CHAT_USER_DATA
import com.gowtham.letschat.utils.Constants.GROUP_DATA
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject


const val TYPE_LOGGED_IN = "new_logged_in"

const val TYPE_NEW_MESSAGE = "new_message"

const val TYPE_NEW_GROUP = "new_group"

const val TYPE_NEW_GROUP_MESSAGE = "new_group_message"

const val GROUP_KEY = "com.mygroupkey"

const val SUMMARY_ID = 0

const val KEY_TEXT_REPLY = "key_text_reply"

@AndroidEntryPoint
class FirebasePush : FirebaseMessagingService(), OnSuccessListener {

    @Inject
    lateinit var preference: MPreference

    @Inject
    lateinit var dbRepository: DbRepository

    @Inject
    lateinit var usersCollection: CollectionReference

    @Inject
    lateinit var messageStatusUpdater: MessageStatusUpdater

    @Inject
    lateinit var groupMessageStatusUpdater: GroupMsgStatusUpdater

    @GroupCollection
    @Inject
    lateinit var groupCollection: CollectionReference

    private var sentTime: Long? = null

    private lateinit var pushMsg: PushMsg

    private var userId: String? = null

    private lateinit var messagesOfChatUser: List<Message>

    override fun onCreate() {
        super.onCreate()
        userId = preference.getUid()
    }

    override fun onNewToken(token: String) {
        preference.updatePushToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {
            LogMessage.v("Data Payload: ${remoteMessage.data}")
            if (preference.isNotLoggedIn() || !preference.isSameDevice())
                return
            sentTime = remoteMessage.sentTime
            val data = remoteMessage.data
            pushMsg = Json.decodeFromString(data["data"].toString())
            /* pushMsg.to?.let {
                 if (it!=userId)
                     return
             }*/
            handleNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNotification() {
        when (pushMsg.type) {
            TYPE_LOGGED_IN -> {
                preference.setLastDevice(false)
                val intent = Intent(ACTION_LOGGED_IN_ANOTHER_DEVICE)
                sendBroadcast(intent)
            }
            TYPE_NEW_MESSAGE -> {
                handleNewMessage()
            }
            TYPE_NEW_GROUP -> {
                handleNewGroup()
            }
            TYPE_NEW_GROUP_MESSAGE -> {
                handleGroupMsg()
            }
            else -> {

            }
        }
    }

    private fun handleGroupMsg() {
        //it would be updated by snapshot listeners when app is alive
        if (!MApplication.isAppRunning) {
            val message = Json.decodeFromString<GroupMessage>(pushMsg.message_body.toString())
            CoroutineScope(Dispatchers.IO).launch {
                dbRepository.insertMessage(message)
                val group = dbRepository.getGroupById(message.groupId)
                val messages = dbRepository.getChatsOfGroupList(group?.id.toString())
                if (group != null) {
                    group.unRead = messages.filter {
                        it.from != userId &&
                                Utils.myIndexOfStatus(userId!!, it) < 3
                    }.size
                    dbRepository.insertGroup(group)

                    withContext(Dispatchers.Main) {
                        showGroupNotification(this@FirebasePush, dbRepository)
                        //update delivery status
                        groupMessageStatusUpdater.updateToDelivery(userId!!, messages, group.id)
                    }
                } else {
                    val groupQuery = GroupQuery(message.groupId, dbRepository, preference)
                    groupQuery.getGroupData(groupCollection)
                }
            }
        }
    }

    private fun handleNewGroup() {
        //it would be updated by snapshot listeners when app is alive
        if (!MApplication.isAppRunning) {
            val group = Json.decodeFromString<Group>(pushMsg.message_body.toString())
            val groupQuery = GroupQuery(group.id, dbRepository, preference)
            groupQuery.getGroupData(groupCollection)
        }
    }

    private fun handleNewMessage() {
        val message = Json.decodeFromString<Message>(pushMsg.message_body.toString())
        if (message.to != userId || MApplication.isAppRunning) {
            Timber.v("Push notification ignored")
            return
        }
        val chatUserId = UserUtils.getChatUserId(userId!!, message)  //chatUserId from message
        message.chatUserId = chatUserId
        CoroutineScope(Dispatchers.IO).launch {
            dbRepository.insertMessage(message)
            val chatUser = dbRepository.getChatUserById(chatUserId)
            messagesOfChatUser = dbRepository.getChatsOfFriend(chatUserId)
                .filter { it.to == userId && it.status < 3 }
            if (chatUser != null) {
                chatUser.unRead = messagesOfChatUser.size  //set unread msg count
                dbRepository.insertUser(chatUser)
                withContext(Dispatchers.Main) {
                    showNotification(this@FirebasePush, dbRepository)
                    //update delivery status
                    messageStatusUpdater.updateToDelivery(messagesOfChatUser, chatUser)
                }
            } else {
                withContext(Dispatchers.Main) {
                    //update delivery status in listener
                    val util = ChatUserUtil(dbRepository, usersCollection, this@FirebasePush)
                    util.queryNewUserProfile(
                        this@FirebasePush,
                        chatUserId,
                        null,
                        showNotification = true
                    )
                }
            }
        }
    }

    private suspend fun getBitmap(url: String): Bitmap {
        val loader = ImageLoader(this)
        val request = ImageRequest.Builder(this)
            .data(url)
            .build()
        val result = (loader.execute(request) as SuccessResult).drawable
        return (result as BitmapDrawable).bitmap
    }

    companion object {
        //notification method for common use
        var messageCount = 0
        var personCount = 0

        fun showGroupNotification(context: Context, dbRepository: DbRepository) {
            CoroutineScope(Dispatchers.IO).launch {
                var groupWithMsgs = dbRepository.getGroupWithMessagesList()
                groupWithMsgs = groupWithMsgs.filter { it.group.unRead != 0 }
                checkGroupMessages(context, groupWithMsgs)
            }
        }

        fun showNotification(context: Context, dbRepository: DbRepository) {
            CoroutineScope(Dispatchers.IO).launch {
                var chatUserWithMessages = dbRepository.getChatUserWithMessagesList()
                chatUserWithMessages = chatUserWithMessages.filter { it.user.unRead != 0 }
                checkMessages(context, chatUserWithMessages)
            }
        }

        private fun checkGroupMessages(context: Context, groupWithMsgs: List<GroupWithMessages>) {
            messageCount = 0
            personCount = 0
            val myUserId = MPreference(context).getUid().toString()
            val manager: NotificationManagerCompat = Utils.returnNManager(context)
            val groupNotifications = ArrayList<Notification>()
            if (!groupWithMsgs.isNullOrEmpty()) {
                for (groupMsg in groupWithMsgs) {
                    /*  if (groupMsg.messages.last().from==myUserId)
                          continue*/
                    personCount += 1
                    val person: Person = Person.Builder().setIcon(null)
                        .setKey(groupMsg.group.id).setName(Utils.getGroupName(groupMsg.group.id))
                        .build()
                    val builder = Utils.createBuilder(context, manager)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setStyle(
                            NotificationUtils.getGroupStyle(
                                context,
                                myUserId,
                                person,
                                groupMsg
                            )
                        )
                        .setContentIntent(
                            NotificationUtils.getGroupMsgIntent(
                                context,
                                groupMsg.group
                            )
                        )
                        .setGroup(GROUP_KEY)
                    builder.addAction(
                        R.drawable.ic_drafts,
                        "mark as read",
                        NotificationUtils.getGroupMarkAsPIntent(context, groupMsg)
                    )
                    builder.addAction(NotificationUtils.getGroupReplyAction(context, groupMsg))
                    val notification = builder.build()
                    groupNotifications.add(notification)
                }
            }

            val summaryNotification = NotificationUtils.getSummaryNotification(context, manager)
            for ((index, notification) in groupNotifications.withIndex()) {
                val notIdString = groupWithMsgs[index].group.createdAt.toString()
                val notId = notIdString.substring(notIdString.length - 4)
                    .toInt() //last 4 digits as notificationId
                manager.notify(notId, notification)
            }
            if (groupNotifications.size > 1)
                manager.notify(SUMMARY_ID, summaryNotification)
        }

        private fun checkMessages(
            context: Context,
            chatUserWithMessages: List<ChatUserWithMessages>
        ) {

            if (chatUserWithMessages.isNullOrEmpty())
                return

            messageCount = 0
            personCount = 0
            val notifications = ArrayList<Notification>()
            val myUserId = MPreference(context).getUid().toString()
            val manager: NotificationManagerCompat = Utils.returnNManager(context)

            for (user in chatUserWithMessages) {
                val messages = user.messages.filter { it.status < 3 && it.from != myUserId }
                if (messages.isNullOrEmpty())
                    continue
                personCount += 1
                Timber.v("DocId ${user.user.documentId}")
                val person: Person = Person.Builder().setIcon(null)
                    .setKey(user.user.id).setName(user.user.localName).build()
                val builder = Utils.createBuilder(context, manager)
                    .setStyle(NotificationUtils.getStyle(context, person, user))
                    .setContentIntent(NotificationUtils.getPIntent(context, user.user))
                    .setGroup(GROUP_KEY)
                if (!user.user.documentId.isNullOrBlank()) {
                    builder.addAction(
                        R.drawable.ic_drafts,
                        "mark as read",
                        NotificationUtils.getMarkAsPIntent(context, user)
                    )
                    builder.addAction(NotificationUtils.getReplyAction(context, user))
                }
                val notification = builder.build()
                notifications.add(notification)
            }

            val summaryNotification = NotificationUtils.getSummaryNotification(context, manager)
            for ((index, notification) in notifications.withIndex()) {
                val notIdString = chatUserWithMessages[index].user.user.createdAt.toString()
                val notId = notIdString.substring(notIdString.length - 4)
                    .toInt() //last 4 digits as notificationId
                manager.notify(notId, notification)
            }

            if (notifications.size > 1)
                manager.notify(SUMMARY_ID, summaryNotification)
        }

    }

    override fun onResult(success: Boolean, data: Any?) {
        if (success) {
            messageStatusUpdater.updateToDelivery(messagesOfChatUser, data as ChatUser)
        }
    }
}