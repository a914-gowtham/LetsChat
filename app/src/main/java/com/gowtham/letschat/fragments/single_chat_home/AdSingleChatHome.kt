package com.gowtham.letschat.fragments.single_chat_home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gowtham.letschat.databinding.RowChatBinding
import com.gowtham.letschat.databinding.RowReceiveMessageBinding
import com.gowtham.letschat.databinding.RowSentMessageBinding
import com.gowtham.letschat.db.data.ChatUserWithMessages
import com.gowtham.letschat.utils.ItemClickListener
import com.gowtham.letschat.utils.MPreference
import java.util.*

class AdSingleChatHome(private val context: Context) :
    ListAdapter<ChatUserWithMessages, RecyclerView.ViewHolder>(DiffCallbackChats()) {

    private val preference = MPreference(context)

    companion object {
        lateinit var allChatList: MutableList<ChatUserWithMessages>
        lateinit var itemClickListener: ItemClickListener
    }

    fun filter(query: String) {
        try {
            val list= mutableListOf<ChatUserWithMessages>()
            if (query.isEmpty())
                list.addAll(allChatList)
            else {
                for (contact in allChatList) {
                    if (contact.user.localName.toLowerCase(Locale.getDefault())
                            .contains(query.toLowerCase(Locale.getDefault()))) {
                        list.add(contact)
                    }
                }
            }
            submitList(null)
            submitList(list)
        } catch (e: Exception) {
            e.stackTrace
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RowChatBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder=holder as ViewHolder
        viewHolder.bind(getItem(position))
    }

    class ViewHolder(val binding: RowChatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatUserWithMessages) {
            binding.chatUser = item
            binding.viewRoot.setOnClickListener { v ->
                itemClickListener.onItemClicked(v,bindingAdapterPosition)
            }
            binding.executePendingBindings()
        }
    }

}

class DiffCallbackChats : DiffUtil.ItemCallback<ChatUserWithMessages>() {
    override fun areItemsTheSame(oldItem: ChatUserWithMessages, newItem: ChatUserWithMessages): Boolean {
        return oldItem.user.id == oldItem.user.id
    }

    override fun areContentsTheSame(oldItem: ChatUserWithMessages, newItem: ChatUserWithMessages): Boolean {
        return oldItem.messages == newItem.messages && oldItem.user==newItem.user
    }
}
