package com.gowtham.letschat.models

data class UserStatus (val status: String="online",val last_seen: Long=0,
                       val typing_status: String="non_typing",val chatuser: String?=null) {
}