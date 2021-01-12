package com.gowtham.letschat.db.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation

@kotlinx.parcelize.Parcelize
class GroupWithMessages (
    @Embedded
    val group: Group,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val messages: List<GroupMessage>) : Parcelable