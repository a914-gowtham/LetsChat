package com.gowtham.letschat.fragments.single_chat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gowtham.letschat.databinding.*
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.fragments.group_chat.AdGroupChat
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
        private const val TYPE_STICKER_SENT = 4
        private const val TYPE_STICKER_RECEIVE = 5
        lateinit var messageList: MutableList<Message>
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TXT_SENT -> {
                val binding = RowSentMessageBinding.inflate(layoutInflater, parent, false)
                TxtSentVHolder(binding)
            }
            TYPE_TXT_RECEIVED-> {
                val binding = RowReceiveMessageBinding.inflate(layoutInflater, parent, false)
                TxtReceiveVHolder(binding)
            }
            TYPE_IMG_SENT-> {
                val binding = RowImageSentBinding.inflate(layoutInflater, parent, false)
                ImageSentVHolder(binding)
            }
            TYPE_IMG_RECEIVE-> {
                val binding = RowImageReceiveBinding.inflate(layoutInflater, parent, false)
                ImageReceiveVHolder(binding)
            }
            TYPE_STICKER_SENT-> {
                val binding = RowStickerSentBinding.inflate(layoutInflater, parent, false)
                StickerSentVHolder(binding)
            }
            TYPE_STICKER_RECEIVE-> {
                val binding = RowStickerReceiveBinding.inflate(layoutInflater, parent, false)
                StickerReceiveVHolder(binding)
            }
            else-> {
                val binding = RowStickerReceiveBinding.inflate(layoutInflater, parent, false)
                StickerReceiveVHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is TxtSentVHolder ->
                holder.bind(getItem(position))
            is TxtReceiveVHolder ->
                holder.bind(getItem(position))
            is ImageSentVHolder ->
                holder.bind(getItem(position))
            is ImageReceiveVHolder ->
                holder.bind(getItem(position))
            is StickerSentVHolder ->
                holder.bind(getItem(position))
            is StickerReceiveVHolder ->
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
        else if (fromMe && message.type == "image" && message.imageMessage?.imageType=="image")
            return TYPE_IMG_SENT
        else if (!fromMe && message.type == "image" && message.imageMessage?.imageType=="image")
            return TYPE_IMG_RECEIVE
        else if (fromMe && message.type == "image" && (message.imageMessage?.imageType=="sticker"
                    || message.imageMessage?.imageType=="gif"))
            return TYPE_STICKER_SENT
        else if (!fromMe && message.type == "image"  && (message.imageMessage?.imageType=="sticker"
                    || message.imageMessage?.imageType=="gif"))
            return TYPE_STICKER_RECEIVE
        return super.getItemViewType(position)
    }


    class TxtSentVHolder(val binding: RowSentMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class TxtReceiveVHolder(val binding: RowReceiveMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class ImageSentVHolder(val binding: RowImageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class ImageReceiveVHolder(val binding: RowImageReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class StickerSentVHolder(val binding: RowStickerSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class StickerReceiveVHolder(val binding: RowStickerReceiveBinding) :
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
