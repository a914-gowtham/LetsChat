package com.gowtham.letschat.fragments.add_group_members

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gowtham.letschat.databinding.RowAddMemberBinding
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.utils.DiffCallbackChatUser
import com.gowtham.letschat.utils.ItemClickListener
import java.util.*
import kotlin.collections.ArrayList

class AdAddMembers(private val context: Context) :
    ListAdapter<ChatUser, RecyclerView.ViewHolder>(DiffCallbackChatUser()) {


    companion object {
        var allContacts = ArrayList<ChatUser>()
        lateinit var listener: ItemClickListener
    }

    fun filter(query: String) {
        val list = ArrayList<ChatUser>()
        if (query.isEmpty()) {
            list.addAll(allContacts)
        } else {
            val queryList = allContacts.filter {
                it.localName.toLowerCase(Locale.getDefault())
                    .contains(query.toLowerCase(Locale.getDefault()))
            }
            list.addAll(queryList)
        }
        submitList(list as MutableList<ChatUser>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RowAddMemberBinding.inflate(layoutInflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as MyViewHolder
        holder.bind(getItem(position))
    }

    override fun submitList(list: List<ChatUser>?) {
        super.submitList(list?.let { ArrayList(it) })
    }

    class MyViewHolder(val binding: RowAddMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatUser) {
            binding.chatUser = item
            binding.viewRoot.setOnClickListener {
                listener.onItemClicked(it, bindingAdapterPosition)
            }
            binding.executePendingBindings()
        }
    }
}