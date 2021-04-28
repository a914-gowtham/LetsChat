package com.gowtham.letschat.services

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.CollectionReference
import com.gowtham.letschat.TYPE_NEW_GROUP_MESSAGE
import com.gowtham.letschat.core.GroupMsgSender
import com.gowtham.letschat.core.OnGrpMessageResponse
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.di.GroupCollection
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
class GroupUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    @GroupCollection
    val groupCollection: CollectionReference,
    private val dbRepository: DbRepository):
    Worker(appContext, workerParams) {

    private val params=workerParams

    override fun doWork(): Result {
        val stringData=params.inputData.getString(Constants.MESSAGE_DATA) ?: ""
        val message= Json.decodeFromString<GroupMessage>(stringData)

        val url=params.inputData.getString(Constants.MESSAGE_FILE_URI)!!
        val sourceName=getSourceName(message,url)
        val storageRef=UserUtils.getStorageRef(applicationContext)
        val child = storageRef.child(
            "group/${message.to}/$sourceName")
       val task = if(url.contains(".mp3")) {
            val stream = FileInputStream(url)  //audio message
            child.putStream(stream)
        }else
            child.putFile(Uri.parse(message.imageMessage?.uri))

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
                message.status[0]=4
                dbRepository.insertMessage(message)
                countDownLatch.countDown()
            }
        }
        countDownLatch.await()
        return result[0]
    }

    private fun sendMessage(
        message: GroupMessage,imgUrl: String,
        result: Array<Result>,
        countDownLatch: CountDownLatch) {
        val group=Json.decodeFromString<Group>(params.inputData.getString(Constants.GROUP_DATA)!!)
        setUrl(message,imgUrl)
        val messageSender = GroupMsgSender(groupCollection)
        messageSender.sendMessage(message, group, object : OnGrpMessageResponse{
            override fun onSuccess(message: GroupMessage) {
                sendPushToMembers(group,message)
                result[0]= Result.success()
                countDownLatch.countDown()
            }

            override fun onFailed(message: GroupMessage) {
                result[0]= Result.failure()
                dbRepository.insertMessage(message)
                countDownLatch.countDown()
            }
        })
    }

    private fun setUrl(message: GroupMessage, imgUrl: String) {
        if (message.type=="audio")
            message.audioMessage?.uri=imgUrl
        else
            message.imageMessage?.uri=imgUrl
    }

    private fun sendPushToMembers(group: Group, message: GroupMessage) {
        val users = group.members?.filter { it.user.token.isNotEmpty() }?.map {
            it.user.token
            it
        }
        users?.forEach {
            UserUtils.sendPush(
                applicationContext, TYPE_NEW_GROUP_MESSAGE,
                Json.encodeToString(message), it.user.token, it.id
            )
        }
    }

    private fun getSourceName(message: GroupMessage, url: String): String {
        val createdAt=message.createdAt.toString()
        val num=createdAt.substring(createdAt.length - 5)
        val extension=url.substring(url.lastIndexOf('.'))
        return "${message.type}_$num$extension"
    }

}
