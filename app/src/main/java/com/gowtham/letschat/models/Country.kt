package com.gowtham.letschat.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Country(
    val code: String, val name: String, val noCode: String,
    val money: String
) : Parcelable
