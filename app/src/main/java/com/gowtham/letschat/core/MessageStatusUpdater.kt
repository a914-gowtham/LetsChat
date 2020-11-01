package com.gowtham.letschat.core

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.fragments.single_chat.serializeToMap
import com.gowtham.letschat.utils.LogMessage
import timber.log.Timber

class MessageStatusUpdater(private val msgCollection: CollectionReference) {

    fun updateSingleMsgStatus(isDelivery: Boolean,fromUser: String,docId: String,message: Message){
        val msgSubCollection=msgCollection.document(docId).collection("messages")
        if (message.to ==fromUser){
            message.chatUserId=null
            message.status=if (isDelivery) 2 else 3  //changing status to delivered
            val currentTime = System.currentTimeMillis()
            message.deliveryTime = message.deliveryTime ?: currentTime
            if (isDelivery)
             message.seenTime = currentTime
            msgSubCollection.document(message.createdAt.toString()).update(message.serializeToMap())
        }
    }
    
    fun updateToDelivery(myUserId: String, messageList: List<Message>, vararg chatUsers: ChatUser) {
        val batch= FirebaseFirestore.getInstance().batch()
        for (chatUser in chatUsers){
            if (chatUser.documentId.isNullOrBlank())
                continue
            val msgSubCollection=msgCollection.document(chatUser.documentId!!).collection("messages")
        val filterList=  messageList
            .filter { msg-> msg.to==myUserId && msg.status==1 && msg.from==chatUser.id}
            .map {
                it.chatUserId=null
                it.status=2
                it.deliveryTime=System.currentTimeMillis()
                it
            }
        if (filterList.isNotEmpty()){
            for (msg in filterList){
                batch.update(msgSubCollection
                    .document(msg.createdAt.toString()),msg.serializeToMap())
            }
        }}
        batch.commit().addOnSuccessListener {
            LogMessage.v("Batch update success from home")
        }.addOnFailureListener {
            LogMessage.v("Batch update failure ${it.message} from home")
        }
    }

    fun updateToSeen(fromUser: String,toUser: String,docId: String, messageList: List<Message>) {
        val msgSubCollection = msgCollection.document(docId).collection("messages")
        val batch = FirebaseFirestore.getInstance().batch()
        val currentTime = System.currentTimeMillis()
        val filterList=  messageList
            .filter { msg-> msg.to == fromUser && msg.from==toUser && msg.status != 3 }
            .map {
            it.status=3
            it.chatUserId=null
            it.deliveryTime = it.deliveryTime ?: currentTime
            it.seenTime = currentTime
            it
        }
        if (filterList.isNotEmpty()){
            Timber.v("Size of list ${filterList.last().createdAt}")
            for (message in filterList){
                batch.update(
                    msgSubCollection
                        .document(message.createdAt.toString()), message.serializeToMap())
            }
            batch.commit().addOnSuccessListener {
                LogMessage.v("All Message Seen Batch update success")
            }.addOnFailureListener {
                LogMessage.v("All Message Seen Batch update failure ${it.message}")
            }
        }else
            LogMessage.v("All message already seen")
    }
}