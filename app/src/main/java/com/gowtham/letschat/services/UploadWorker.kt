package com.gowtham.letschat.services

import android.content.Context
import android.net.Uri
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.storage.StorageReference
import com.gowtham.letschat.TYPE_NEW_MESSAGE
import com.gowtham.letschat.core.MessageSender
import com.gowtham.letschat.core.OnMessageResponse
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.di.MessageCollection
import com.gowtham.letschat.utils.Constants
import com.gowtham.letschat.utils.UserUtils
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.CountDownLatch

class UploadWorker @WorkerInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val storageRef: StorageReference,
    @MessageCollection
    val msgCollection: CollectionReference,
    val dbRepository: DbRepository):
    Worker(appContext, workerParams) {

    private val params=workerParams

    override fun doWork(): Result {
        val stringData=params.inputData.getString(Constants.MESSAGE_DATA) ?: ""
        val message= Json.decodeFromString<Message>(stringData)

        val createdAt=message.createdAt.toString()
        val num=createdAt.substring(createdAt.length - 5)
        val url=message.imageMessage?.uri.toString()
        val format=url.substring(url.lastIndexOf('.'))
        val sourceName="${message.imageMessage?.imageType}$num$format"

        val child = storageRef.child(
            "chats/${message.to}/$sourceName")
        val task = child.putFile(Uri.parse(url))

        val countDownLatch = CountDownLatch(1)
        val result= arrayOf(Result.failure())
        task.addOnSuccessListener {
            child.downloadUrl.addOnCompleteListener { taskResult ->
                Timber.v("TaskResult ${taskResult.result.toString()}")
                val imgUrl=taskResult.result.toString()
                sendMessage(message,imgUrl,result,countDownLatch)
            }.addOnFailureListener { e ->
                Timber.v("TaskResult Failed ${e.message}")
                result[0]= Result.failure()
                message.status=4
                dbRepository.insertMessage(message)
                countDownLatch.countDown()
            }
        }.addOnProgressListener { taskSnapshot ->
            val progress: Double =
                100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
        }
        countDownLatch.await()
        return result[0]
    }

    private fun sendMessage(message: Message,imgUrl: String,result: Array<Result>,
        countDownLatch: CountDownLatch) {
        val chatUser=Json.decodeFromString<ChatUser>(params.inputData.getString(Constants.CHAT_USER_DATA)!!)
        message.imageMessage?.uri=imgUrl
        val messageSender = MessageSender(
            msgCollection,
            dbRepository,
            chatUser,object : OnMessageResponse{
                override fun onSuccess(message: Message) {
                    UserUtils.sendPush(applicationContext, TYPE_NEW_MESSAGE, Json.encodeToString(message)
                        , chatUser.user.token,message.to)
                    result[0]= Result.success()
                    countDownLatch.countDown()
                }

                override fun onFailed(message: Message) {
                    result[0]= Result.failure()
                    dbRepository.insertMessage(message)
                    countDownLatch.countDown()
                }
            }
        )
        messageSender.checkAndSend(message.from, message.to, message)
    }


}
