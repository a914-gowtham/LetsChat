package com.gowtham.letschat.models

import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ModelMobile(
    var country: String="", var number: String=""): Parcelable
