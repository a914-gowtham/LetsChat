package com.gowtham.letschat.models

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@IgnoreExtraProperties
@Parcelize
data class UserProfile(var uId: String?=null,var createdAt: Long?=null,
                       var updatedAt: Long?=null,
                       var image: String="", var userName: String="",
                       var about: String="",
                       var token :String="",
                       var mobile: ModelMobile?=null,
                       @get:PropertyName("device_details")
                       @set:PropertyName("device_details")
                       var deviceDetails: ModelDeviceDetails?=null) : Parcelable