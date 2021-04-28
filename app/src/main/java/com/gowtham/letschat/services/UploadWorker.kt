package com.gowtham.letschat.services

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.storage.UploadTask
import com.gowtham.letschat.TYPE_NEW_MESSAGE
import com.gowtham.letschat.core.MessageSender
import com.gowtham.letschat.core.OnMessageResponse
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.di.MessageCollection
import com.gowtham.letschat.utils.Constants
import com.gowtham.letschat.utils.UserUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.FileInputStream
import java.util.concurrent.CountDownLatch

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    @MessageCollection
    val msgCollection: CollectionReference,
    val dbRepository: DbRepository):
    Worker(appContext, workerParams) {

    private val params=workerParams

    override fun doWork(): Result {
        val stringData=params.inputData.getString(Constants.MESSAGE_DATA) ?: ""
        val message= Json.decodeFromString<Message>(stringData)

        val url=params.inputData.getString(Constants.MESSAGE_FILE_URI)!!
        val sourceName=getSourceName(message,url)
        val storageRef=UserUtils.getStorageRef(applicationContext)

        val child = storageRef.child(
            "chats/${message.to}/$sourceName")
        val task: UploadTask
        task = if(url.contains(".mp3")) {
            val stream = FileInputStream(url)  //audio message
            child.putStream(stream)
        }else
            child.putFile(Uri.parse(message.imageMessage?.uri))

        val countDownLatch = CountDownLatch(1)
        val result= arrayOf(Result.failure())
        task.addOnSuccessListener {
            child.downloadUrl.addOnCompleteListener { taskResult ->
                Timber.v("TaskResult ${taskResult.result.toString()}")
                val downloadUrl=taskResult.result.toString()
                sendMessage(message,downloadUrl,result,countDownLatch)
            }.addOnFailureListener { e ->
                Timber.v("TaskResult Failed ${e.message}")
                result[0]= Result.failure()
                message.status=4
                dbRepository.insertMessage(message)
                countDownLatch.countDown()
            }
        }
        countDownLatch.await()
        return result[0]
    }

    private fun getSourceName(message: Message, url: String): String {
        val createdAt=message.createdAt.toString()
        val num=createdAt.substring(createdAt.length - 5)
        val extension=url.substring(url.lastIndexOf('.'))
        return "${message.type}_$num$extension"
    }

    private fun sendMessage(message: Message,downloadUrl: String,result: Array<Result>,
        countDownLatch: CountDownLatch) {
        val chatUser=Json.decodeFromString<ChatUser>(params.inputData.getString(Constants.CHAT_USER_DATA)!!)
        setUrl(message,downloadUrl)
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

    private fun setUrl(message: Message, imgUrl: String) {
        if (message.type=="audio")
            message.audioMessage?.uri=imgUrl
        else
            message.imageMessage?.uri=imgUrl
    }


}
