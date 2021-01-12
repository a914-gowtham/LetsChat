package com.gowtham.letschat.db.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@kotlinx.parcelize.Parcelize
@Entity
data class GroupMessage(@PrimaryKey
                        val createdAt: Long, var groupId: String,
                        val from: String, val to: ArrayList<String>,
                        val senderName: String,
                        val senderImage: String,
                        val status: ArrayList<Int>,//0 th index is status of from user
                        val deliveryTime: ArrayList<Long>,
                        val seenTime: ArrayList<Long>,
                        var type: String="text",//0=text,1=audio,2=image,3=video,4=file,5=s_image
                        var textMessage: TextMessage?=null,
                        var imageMessage: ImageMessage?=null,
                        var audioMessage: AudioMessage?=null,
                        var videoMessage: VideoMessage?=null,
                        var fileMessage: FileMessage?=null): Parcelable