package com.gowtham.letschat.db.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.android.parcel.Parcelize

@Parcelize
class ChatUserWithMessages(
    @Embedded
    val user: ChatUser,
    @Relation(
        parentColumn = "id",
        entityColumn = "chatUserId"
    )
    val messages: List<Message>) : Parcelable