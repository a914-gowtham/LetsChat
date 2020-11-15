package com.gowtham.letschat.fragments.group_chat_home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gowtham.letschat.databinding.RowChatBinding
import com.gowtham.letschat.databinding.RowGroupChatBinding
import com.gowtham.letschat.databinding.RowReceiveMessageBinding
import com.gowtham.letschat.databinding.RowSentMessageBinding
import com.gowtham.letschat.db.data.ChatUserWithMessages
import com.gowtham.letschat.db.data.GroupWithMessages
import com.gowtham.letschat.fragments.single_chat_home.AdSingleChatHome
import com.gowtham.letschat.utils.ItemClickListener
import com.gowtham.letschat.utils.MPreference
import java.util.*

class AdGroupChatHome(private val context: Context) :
    ListAdapter<GroupWithMessages, RecyclerView.ViewHolder>(DiffCallbackChats()) {

    private val preference = MPreference(context)

    companion object {
        lateinit var allList: MutableList<GroupWithMessages>
        lateinit var itemClickListener: ItemClickListener
    }

    fun filter(query: String) {
        try {
            val list= mutableListOf<GroupWithMessages>()
            if (query.isEmpty())
                list.addAll(allList)
            else {
                for (group in allList) {
                    if (group.group.id.toLowerCase(Locale.getDefault())
                            .contains(query.toLowerCase(Locale.getDefault()))) {
                        list.add(group)
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
        val binding = RowGroupChatBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder=holder as ViewHolder
        viewHolder.bind(getItem(position))
    }

    class ViewHolder(val binding: RowGroupChatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupWithMessages) {
            binding.groupChat = item
            binding.viewRoot.setOnClickListener { v ->
                itemClickListener.onItemClicked(v,bindingAdapterPosition)
            }
            binding.executePendingBindings()
        }
    }

}

class DiffCallbackChats : DiffUtil.ItemCallback<GroupWithMessages>() {
    override fun areItemsTheSame(oldItem: GroupWithMessages, newItem: GroupWithMessages): Boolean {
        return oldItem.group.id == oldItem.group.id
    }

    override fun areContentsTheSame(oldItem: GroupWithMessages, newItem: GroupWithMessages): Boolean {
        return oldItem.messages == newItem.messages && oldItem.group==newItem.group
    }
}
