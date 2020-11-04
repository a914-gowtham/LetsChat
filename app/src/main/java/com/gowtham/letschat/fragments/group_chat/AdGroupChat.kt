package com.gowtham.letschat.fragments.group_chat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gowtham.letschat.databinding.*
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.fragments.single_chat.AdChat
import com.gowtham.letschat.utils.ItemClickListener
import com.gowtham.letschat.utils.MPreference

class AdGroupChat (private val context: Context,
                   private val msgClickListener: ItemClickListener) :
    ListAdapter<GroupMessage, RecyclerView.ViewHolder>(DiffCallbackMessages()) {

    private val preference = MPreference(context)

    companion object {
        private const val TYPE_TXT_SENT = 0
        private const val TYPE_TXT_RECEIVED = 1
        private const val TYPE_IMG_SENT = 2
        private const val TYPE_IMG_RECEIVE = 3
        lateinit var messageList: MutableList<GroupMessage>
        lateinit var chatUserList: MutableList<ChatUser>
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TXT_SENT -> {
                val binding = RowGroupTxtSentBinding.inflate(layoutInflater, parent, false)
                TxtSentMsgHolder(binding)
            }
            TYPE_TXT_RECEIVED-> {
                val binding = RowGrpTxtReceiveBinding.inflate(layoutInflater, parent, false)
                TxtReceivedMsgHolder(binding)
            }
            TYPE_IMG_SENT -> {
                val binding = RowGroupImageSentBinding.inflate(layoutInflater, parent, false)
                ImgSentMsgHolder(binding)
            }
            else-> {
                val binding = RowGroupImageReceiveBinding.inflate(layoutInflater, parent, false)
                ImgReceivedMsgHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is TxtSentMsgHolder ->{
                holder.bind(getItem(position))
            }
            is TxtReceivedMsgHolder ->
                holder.bind(getItem(position))
            is ImgSentMsgHolder ->
                holder.bind(getItem(position))
            is ImgReceivedMsgHolder ->
                holder.bind(getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val fromMe=message.from == preference.getUid()
        if (fromMe && message.type == "text")
            return TYPE_TXT_SENT
        else if (!fromMe && message.type == "text")
            return TYPE_TXT_RECEIVED
        else if (fromMe && message.type == "image")
            return TYPE_IMG_SENT
        else if (!fromMe && message.type == "image")
            return TYPE_IMG_RECEIVE
        return super.getItemViewType(position)
    }


    class TxtSentMsgHolder(val binding: RowGroupTxtSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class TxtReceivedMsgHolder(val binding: RowGrpTxtReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage) {
            binding.message = item
            binding.chatUsers= chatUserList.toTypedArray()
            binding.executePendingBindings()
        }
    }

    class ImgSentMsgHolder(val binding: RowGroupImageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class ImgReceivedMsgHolder(val binding: RowGroupImageReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage) {
            binding.message = item
            binding.chatUsers= chatUserList.toTypedArray()
            binding.executePendingBindings()
        }
    }
}

class DiffCallbackMessages : DiffUtil.ItemCallback<GroupMessage>() {
    override fun areItemsTheSame(oldItem: GroupMessage, newItem: GroupMessage): Boolean {
        return oldItem.createdAt == newItem.createdAt
    }

    override fun areContentsTheSame(oldItem: GroupMessage, newItem: GroupMessage): Boolean {
        return oldItem == newItem
    }
}
