package com.gowtham.letschat.db.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.gowtham.letschat.models.UserProfile
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@kotlinx.parcelize.Parcelize
@Entity
data class ChatUser(
    @PrimaryKey
    var id: String,var localName: String,var user: UserProfile,
    var documentId: String?=null,var locallySaved: Boolean=false,
    var unRead: Int=0,var isSearchedUser: Boolean=false,var isSelected: Boolean=false): Parcelable