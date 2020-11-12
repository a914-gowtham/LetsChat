package com.gowtham.letschat.core

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.db.data.Message

interface OnGrpMessageResponse{
    fun onSuccess(message: GroupMessage)
    fun onFailed(message: GroupMessage)
}

class GroupMsgSender(private val groupCollection: CollectionReference) {

    fun sendMessage(message: GroupMessage,group: Group,listener: OnGrpMessageResponse){
        message.status[0]=1
        groupCollection.document(group.id).collection("group_messages")
            .document(message.createdAt.toString()).set(message, SetOptions.merge())
            .addOnSuccessListener {
                listener.onSuccess(message)
            }.addOnFailureListener {
                message.status[0]=4
                listener.onFailed(message)
            }
    }

}