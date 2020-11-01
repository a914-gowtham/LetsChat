package com.gowtham.letschat.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ModelMobile(
    var country: String="", var number: String=""): Parcelable
