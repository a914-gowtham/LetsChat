package com.gowtham.letschat.fragments.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gowtham.letschat.databinding.RowSearchContactBinding
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.ItemClickListener

class AdSearch(private val listener: ItemClickListener) :
              PagingDataAdapter<UserProfile, AdSearch.MyViewHolder>(MY_COMPARATOR){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            RowSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem!!)
    }

    inner class MyViewHolder(private val binding: RowSearchContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null)
                            listener.onItemClicked(it,position)
                    }
                }
            }

             fun bind(item: UserProfile){
                 binding.profile=item
                 binding.executePendingBindings()
             }
        }


    companion object {
        private val MY_COMPARATOR = object : DiffUtil.ItemCallback<UserProfile>() {
            override fun areItemsTheSame(oldItem: UserProfile, newItem: UserProfile) =
                oldItem.uId == newItem.uId

            override fun areContentsTheSame(oldItem: UserProfile, newItem: UserProfile) =
                oldItem == newItem
        }
    }
}