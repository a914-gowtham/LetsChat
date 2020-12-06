package com.gowtham.letschat.fragments.group_chat

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.*
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.fragments.single_chat.AdChat
import com.gowtham.letschat.utils.Events.EventAudioMsg
import com.gowtham.letschat.utils.ItemClickListener
import com.gowtham.letschat.utils.MPreference
import com.gowtham.letschat.utils.gone
import com.gowtham.letschat.utils.show
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.IOException

class AdGroupChat (private val context: Context,
                   private val msgClickListener: ItemClickListener) :
    ListAdapter<GroupMessage, RecyclerView.ViewHolder>(DiffCallbackMessages()) {

    private val preference = MPreference(context)

    companion object {
        private const val TYPE_TXT_SENT = 0
        private const val TYPE_TXT_RECEIVED = 1
        private const val TYPE_IMG_SENT = 2
        private const val TYPE_IMG_RECEIVE = 3
        private const val TYPE_STICKER_SENT = 4
        private const val TYPE_STICKER_RECEIVE = 5
        private const val TYPE_AUDIO_SENT = 6
        private const val TYPE_AUDIO_RECEIVE = 7
        lateinit var messageList: MutableList<GroupMessage>
        lateinit var chatUserList: MutableList<ChatUser>
        private var player = MediaPlayer()
        private var lastPlayedHolder: RowGroupAudioSentBinding?=null
        private var lastReceivedPlayedHolder: RowGroupAudioReceiveBinding?=null
        private var lastPlayedAudioId : Long=-1

        fun stopPlaying() {
            if(player.isPlaying) {
                lastReceivedPlayedHolder?.progressBar?.abandon()
                lastPlayedHolder?.progressBar?.abandon()
                lastReceivedPlayedHolder?.imgPlay?.setImageResource(R.drawable.ic_action_play)
                lastPlayedHolder?.imgPlay?.setImageResource(R.drawable.ic_action_play)
                player.apply {
                    stop()
                    reset()
                    EventBus.getDefault().post(EventAudioMsg(false))
                }
            }
        }

        fun isPlaying() = player.isPlaying
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
            TYPE_IMG_RECEIVE->{
                val binding = RowGroupImageReceiveBinding.inflate(layoutInflater, parent, false)
                ImgReceivedMsgHolder(binding)
            }
            TYPE_STICKER_SENT -> {
                val binding = RowGroupStickerSentBinding.inflate(layoutInflater, parent, false)
                StickerSentMsgHolder(binding)
            }
            TYPE_STICKER_RECEIVE-> {
                val binding = RowGroupStickerReceiveBinding.inflate(layoutInflater, parent, false)
                StickerReceivedMsgHolder(binding)
            }
            TYPE_AUDIO_SENT -> {
                val binding = RowGroupAudioSentBinding.inflate(layoutInflater, parent, false)
                AudioSentVHolder(binding)
            }
            else-> {
                val binding = RowGroupAudioReceiveBinding.inflate(layoutInflater, parent, false)
                AudioReceiveVHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is TxtSentMsgHolder ->
                holder.bind(getItem(position))
            is TxtReceivedMsgHolder ->
                holder.bind(getItem(position))
            is ImgSentMsgHolder ->
                holder.bind(getItem(position),msgClickListener)
            is ImgReceivedMsgHolder ->
                holder.bind(getItem(position),msgClickListener)
            is StickerSentMsgHolder ->
                holder.bind(getItem(position))
            is StickerReceivedMsgHolder ->
                holder.bind(getItem(position))
            is AudioSentVHolder ->
                holder.bind(context,getItem(position))
            is AudioReceiveVHolder ->
                holder.bind(context,getItem(position))
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
        else if (!fromMe && message.type == "image"  && message.imageMessage?.imageType=="image")
            return TYPE_IMG_RECEIVE
        else if (fromMe && message.type == "image" && (message.imageMessage?.imageType=="sticker"
                    || message.imageMessage?.imageType=="gif"))
            return TYPE_STICKER_SENT
        else if (!fromMe && message.type == "image"  && (message.imageMessage?.imageType=="sticker"
                    || message.imageMessage?.imageType=="gif"))
            return TYPE_STICKER_RECEIVE
        else if (fromMe && message.type == "audio")
            return TYPE_AUDIO_SENT
        else if (!fromMe && message.type == "audio")
            return TYPE_AUDIO_RECEIVE
        return super.getItemViewType(position)
    }


    class TxtSentMsgHolder(val binding: RowGroupTxtSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage) {
            binding.message = item
            if (bindingAdapterPosition>0) {
                val message = messageList[bindingAdapterPosition - 1]
                if (message.from == item.from) {
                    binding.txtMsg.setBackgroundResource(R.drawable.shape_send_msg_corned)
                }
            }
            binding.executePendingBindings()
        }
    }

    class TxtReceivedMsgHolder(val binding: RowGrpTxtReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage) {
            binding.message = item
            binding.chatUsers= chatUserList.toTypedArray()
            if (bindingAdapterPosition>0) {
                val lastMsg = messageList[bindingAdapterPosition - 1]
                if (lastMsg.from == item.from) {
                    binding.apply {
                        viewDetail.gone()
                    }
                    binding.viewMsgHolder.setBackgroundResource(R.drawable.shape_receive_msg_corned)
                }
            }
            binding.executePendingBindings()
        }
    }

    class ImgSentMsgHolder(val binding: RowGroupImageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage, msgClickListener: ItemClickListener) {
            binding.message = item
            binding.imageMsg.setOnClickListener {
                msgClickListener.onItemClicked(it,bindingAdapterPosition)
            }
            binding.executePendingBindings()
        }
    }

    class ImgReceivedMsgHolder(val binding: RowGroupImageReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage, msgClickListener: ItemClickListener) {
            binding.message = item
            binding.chatUsers= chatUserList.toTypedArray()
            binding.imageMsg.setOnClickListener {
                msgClickListener.onItemClicked(it,bindingAdapterPosition)
            }
            binding.executePendingBindings()
        }
    }

    class StickerSentMsgHolder(val binding: RowGroupStickerSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage) {
            binding.message = item
            binding.executePendingBindings()
        }
    }

    class StickerReceivedMsgHolder(val binding: RowGroupStickerReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage) {
            binding.message = item
            binding.chatUsers= chatUserList.toTypedArray()
            binding.executePendingBindings()
        }
    }

    class AudioReceiveVHolder(val binding: RowGroupAudioReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: GroupMessage) {
            binding.message = item
            binding.progressBar.setStoriesCountDebug(1, 0)
            binding.progressBar.setAllStoryDuration(item.audioMessage?.duration!!.toLong() * 1000)
            binding.imgPlay.setOnClickListener {
                startPlaying(
                    context,
                    item,
                    binding
                )
            }
            binding.executePendingBindings()
        }


        private fun startPlaying(
            context: Context,
            item: GroupMessage,
            currentHolder: RowGroupAudioReceiveBinding
        ) {
            if (player.isPlaying) {
                stopPlaying()
                if (lastPlayedAudioId == item.createdAt)
                    return
            }
            player = MediaPlayer()
            lastReceivedPlayedHolder = currentHolder
            lastPlayedAudioId = item.createdAt
            currentHolder.progressBuffer.show()
            currentHolder.imgPlay.gone()
            player.apply {
                try {
                    setDataSource(context, Uri.parse(item.audioMessage?.uri))
                    prepareAsync()
                    setOnPreparedListener {
                        Timber.v("Started..")
                        start()
                        currentHolder.progressBuffer.gone()
                        currentHolder.imgPlay.setImageResource(R.drawable.ic_action_stop)
                        currentHolder.imgPlay.show()
                        currentHolder.progressBar.startStories()
                        EventBus.getDefault().post(EventAudioMsg(true))
                    }
                    setOnCompletionListener {
                        currentHolder.progressBar.abandon()
                        currentHolder.imgPlay.setImageResource(R.drawable.ic_action_play)
                        EventBus.getDefault().post(EventAudioMsg(false))
                    }
                } catch (e: IOException) {
                    println("ChatFragment.startPlaying:prepare failed")
                }
            }
        }

    }

    class AudioSentVHolder(val binding: RowGroupAudioSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            context: Context,
            item: GroupMessage) {
            binding.message = item
            binding.progressBar.setStoriesCountDebug(1, 0)
            binding.progressBar.setAllStoryDuration(item.audioMessage?.duration!!.toLong() * 1000)
            binding.imgPlay.setOnClickListener {
                startPlaying(
                    context,
                    item,
                    binding
                )
            }
            binding.executePendingBindings()
        }

        private fun startPlaying(
            context: Context,
            item: GroupMessage,
            currentHolder: RowGroupAudioSentBinding) {
            if (player.isPlaying) {
                stopPlaying()
                if (lastPlayedAudioId == item.createdAt)
                    return
            }
            player = MediaPlayer()
            lastPlayedHolder = currentHolder
            lastPlayedAudioId = item.createdAt
            currentHolder.progressBuffer.show()
            currentHolder.imgPlay.gone()
            player.apply {
                try {
                    setDataSource(context, Uri.parse(item.audioMessage?.uri))
                    prepareAsync()
                    setOnPreparedListener {
                        Timber.v("Started..")
                        start()
                        currentHolder.progressBuffer.gone()
                        currentHolder.imgPlay.setImageResource(R.drawable.ic_action_stop)
                        currentHolder.imgPlay.show()
                        currentHolder.progressBar.startStories()
                        EventBus.getDefault().post(EventAudioMsg(true))
                    }
                    setOnCompletionListener {
                        currentHolder.progressBar.abandon()
                        currentHolder.imgPlay.setImageResource(R.drawable.ic_action_play)
                        EventBus.getDefault().post(EventAudioMsg(false))
                    }
                } catch (e: IOException) {
                    println("ChatFragment.startPlaying:prepare failed")
                }
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
}}
