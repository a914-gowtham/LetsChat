package com.gowtham.letschat.db.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Parcelize
@Entity
data class Message(
    @PrimaryKey
    val createdAt: Long, var deliveryTime: Long?=null,
    var seenTime: Long?=null,
    val from: String, val to: String,
    val senderName: String,
    val senderImage: String,
    var type: String="text",//0=text,1=audio,2=image,3=video,4=file
    var status: Int=0,//0=sending,1=sent,2=delivered,3=seen,4=failed
    var textMessage: TextMessage?=null,
    var imageMessage: ImageMessage?=null,
    var audioMessage: AudioMessage?=null,
    var videoMessage: VideoMessage?=null,
    var fileMessage: FileMessage?=null,
    @set:Exclude @get:Exclude
                   var chatUserId: String?=null): Parcelable

@Serializable
@Parcelize
data class TextMessage(val text: String?=null): Parcelable

@Serializable
@Parcelize
data class AudioMessage(var uri: String?=null,val duration: Int=0,
                        var currentDuration: Int=0,var isLoading: Boolean=false,
                        var isPlaying: Boolean=false): Parcelable

@Serializable
@Parcelize
data class ImageMessage(var uri: String?=null,var imageType: String="image"): Parcelable

@Serializable
@Parcelize
data class VideoMessage(val uri: String?=null,val duration: Int=0): Parcelable

@Serializable
@Parcelize
data class FileMessage(val name: String?=null,
                       val uri: String?=null,val duration: Int=0): Parcelable
