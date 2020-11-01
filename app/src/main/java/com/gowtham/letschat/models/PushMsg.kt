package com.gowtham.letschat.models

import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Serializable
@IgnoreExtraProperties
data class PushMsg(var type: String?=null,var to: String?=null,var title: String?=null,
                   var message: String?=null,var message_body: String?=null) {
}