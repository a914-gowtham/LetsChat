package com.gowtham.letschat.core

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.fragments.single_chat.serializeToMap
import com.gowtham.letschat.utils.LogMessage
import com.gowtham.letschat.utils.Utils.myIndexOfStatus
import com.gowtham.letschat.utils.Utils.myMsgStatus

class GroupMsgStatusUpdater(private val groupCollection: CollectionReference) {

    fun updateToDelivery(myUserId: String, messageList: List<GroupMessage>, vararg groupId: String){
        val batch= FirebaseFirestore.getInstance().batch()
        for (id in groupId){
            val msgSubCollection=groupCollection.document(id).collection("group_messages")
            val filterList=  messageList
                .filter { it.from!=myUserId && myMsgStatus(myUserId,it)==0 && it.groupId==id }
                .map {
                    val myIndex=myIndexOfStatus(myUserId,it)
                    it.status[myIndex]=2
                    it.deliveryTime[myIndex]=System.currentTimeMillis()
                    it
                }

            if (filterList.isNotEmpty()){
                for (msg in filterList){
                    batch.update(msgSubCollection
                        .document(msg.createdAt.toString()),msg.serializeToMap())
                }
            }
        }
        batch.commit().addOnSuccessListener {
            LogMessage.v("Batch update success from group")
        }.addOnFailureListener {
            LogMessage.v("Batch update failure ${it.message} from group")
        }
    }

    fun updateToSeen(myUserId: String, messageList: List<GroupMessage>, groupId: String){
        val batch= FirebaseFirestore.getInstance().batch()
        val currentTime = System.currentTimeMillis()
        val msgSubCollection=groupCollection.document(groupId).collection("group_messages")
            val filterList=  messageList
                .filter { it.from!=myUserId && myMsgStatus(myUserId,it)<3 }
                .map {
                    val myIndex=myIndexOfStatus(myUserId,it)
                    it.status[myIndex]=3
                    it.deliveryTime[myIndex]= if (it.deliveryTime[myIndex]==0L)
                        currentTime else it.deliveryTime[myIndex]
                    it.seenTime[myIndex]=currentTime
                    it
                }

            if (filterList.isNotEmpty()){
                for (msg in filterList){
                    batch.update(msgSubCollection
                        .document(msg.createdAt.toString()),msg.serializeToMap())
                }
            }
        batch.commit().addOnSuccessListener {
            LogMessage.v("Seen Batch update success from group")
        }.addOnFailureListener {
            LogMessage.v("Seen Batch update failure ${it.message} from group")
        }
    }
}