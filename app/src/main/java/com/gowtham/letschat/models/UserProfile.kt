package com.gowtham.letschat.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.android.parcel.Parcelize
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