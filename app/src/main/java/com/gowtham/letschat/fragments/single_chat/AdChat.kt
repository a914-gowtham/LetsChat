package com.gowtham.letschat.fragments.single_chat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gowtham.letschat.databinding.RowImageReceiveBinding
import com.gowtham.letschat.databinding.RowImageSentBinding
import com.gowtham.letschat.databinding.RowReceiveMessageBinding
import com.gowtham.letschat.databinding.RowSentMessageBinding
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.utils.ItemClickListener
import com.gowtham.letschat.utils.MPreference

class AdChat(private val context: Context, private val msgClickListener: ItemClickListener) :
    ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallbackMessages()) {

    private val preference = MPreference(context)

    companion object {
        private const val TYPE_TXT_SENT = 0
        private const val TYPE_TXT_RECEIVED = 1
        private const val TYPE_IMG_SENT = 2
        private const val TYPE_IMG_RECEIVE = 3
        lateinit var messageList: MutableList<Message>
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TXT_SENT -> {
                val binding = RowSentMessageBinding.inflate(layoutInflater, parent, false)
                SendMessageViewHolder(binding)
            }
            TYPE_TXT_RECEIVED-> {
                val binding = RowReceiveMessageBinding.inflate(layoutInflater, parent, false)
                ReceiveMessageViewHolder(binding)
            }
            TYPE_IMG_SENT-> {
                val binding = RowImageSentBinding.inflate(layoutInflater, parent, false)
                SendImgVHolder(binding)
            }
            else-> {
                val binding = RowImageReceiveBinding.inflate(layoutInflater, parent, false)
                ReceiveImgVHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is SendMessageViewHolder ->
                holder.bind(getItem(position))
            is ReceiveMessageViewHolder ->
                holder.bind(getItem(position))
            is SendImgVHolder ->
                holder.bind(getItem(position))
            is ReceiveImgVHolder ->
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


    class SendMessageViewHolder(val binding: RowSentMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class ReceiveMessageViewHolder(val binding: RowReceiveMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class SendImgVHolder(val binding: RowImageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class ReceiveImgVHolder(val binding: RowImageReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message) {
            binding.message = item
            binding.executePendingBindings()
        }
    }


}

class DiffCallbackMessages : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.createdAt == newItem.createdAt
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}
