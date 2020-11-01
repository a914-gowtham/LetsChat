package com.gowtham.letschat.utils

import androidx.recyclerview.widget.DiffUtil
import com.gowtham.letschat.db.data.ChatUser

class DiffCallbackChatUser  : DiffUtil.ItemCallback<ChatUser>() {
    override fun areItemsTheSame(oldItem: ChatUser, newItem: ChatUser): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatUser, newItem: ChatUser): Boolean {
        return oldItem.isSelected == newItem.isSelected &&
                oldItem.locallySaved==newItem.locallySaved &&
                oldItem.unRead==newItem.unRead &&
                oldItem.localName ==newItem.localName
    }
}