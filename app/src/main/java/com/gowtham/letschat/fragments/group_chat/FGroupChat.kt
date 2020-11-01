package com.gowtham.letschat.fragments.group_chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.gowtham.letschat.databinding.FGroupChatBinding
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.db.data.TextMessage
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.*
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FGroupChat : Fragment(),ItemClickListener {

    @Inject
    lateinit var groupDao: GroupDao

    @Inject
    lateinit var preference: MPreference

    private val viewModel: GroupChatViewModel by viewModels()

    private lateinit var binding: FGroupChatBinding

    val args by navArgs<FGroupChatArgs>()

    lateinit var group: Group

    private var messageList = mutableListOf<GroupMessage>()

    private lateinit var manager: LinearLayoutManager

    private lateinit var localUserId: String

    private lateinit var fromUser: UserProfile

    private val adChat: AdGroupChat by lazy {
        AdGroupChat(requireContext(),this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding=FGroupChatBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner=viewLifecycleOwner
        binding.viewmodel=viewModel
        group=args.group
        binding.group=group
        binding.viewChatHeader.viewBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.viewChatBtm.lottieSend.setOnClickListener {
            sendMessage()
        }
        UserUtils.setUnReadCountGroup(groupDao, group)
        setDataInView()
        subscribeObservers()
    }

    private fun sendMessage() {
        val msg = binding.viewChatBtm.edtMsg.text.trim().toString()
        if (msg.isEmpty())
            return
        binding.viewChatBtm.lottieSend.playAnimation()
        val messageData = TextMessage(msg)
        val message = createMessage()
        message.textMessage=messageData
        viewModel.sendMessage(message)
        binding.viewChatBtm.edtMsg.setText("")
    }

    private fun createMessage(): GroupMessage {
        val toUsers=group.members?.map { it.id } as ArrayList
        val groupSize=group.members!!.size
        val statusList=ArrayList<Int>()
        val deliveryTimeList=ArrayList<Long>()
        for (index in 0 until groupSize){
            statusList.add(0)
            deliveryTimeList.add(0L)
        }
      return GroupMessage(System.currentTimeMillis(), group.id, from = localUserId,
            to = toUsers, fromUser.userName, fromUser.image.toString(), statusList, deliveryTimeList,
            deliveryTimeList)
    }

    private fun subscribeObservers() {
        viewModel.getChatUsers().observe(viewLifecycleOwner,{ chatUsers->
            AdGroupChat.chatUserList= chatUsers.toMutableList()
        })

         viewModel.getGroupMessages(group.id).observe(viewLifecycleOwner,{ message->
             messageList=message as MutableList<GroupMessage>
             AdGroupChat.messageList = messageList
             adChat.submitList(messageList)
             //scroll to last items in recycler (recent messages)
             if (messageList.isNotEmpty()) {
                 viewModel.setChatsOfThisUser(messageList)
                 if (viewModel.getCanScroll())  //scroll only if new message arrived
                     binding.listMessage.smoothScrollToPos(messageList.lastIndex)
                 else
                     viewModel.canScroll(true)
             }
         })

        viewModel.typingUsers.observe(viewLifecycleOwner,{ typingUser->
            if (typingUser.isEmpty())
                BindingAdapters.setMemberNames(binding.viewChatHeader.txtMembers,group)
            else
                binding.viewChatHeader.txtMembers.text=typingUser
        })
    }

    private fun setDataInView() {
        fromUser = preference.getUserProfile()!!
        localUserId=fromUser.uId!!
        manager= LinearLayoutManager(context)
        binding.listMessage.apply {
            manager.stackFromEnd=true
            layoutManager=manager
            setHasFixedSize(true)
            isNestedScrollingEnabled=false
            itemAnimator = null
        }
        binding.listMessage.adapter = adChat
        adChat.addRestorePolicy()
        viewModel.setGroup(group)
        binding.viewChatBtm.edtMsg.addTextChangedListener(msgTxtChangeListener)
    }

    private val msgTxtChangeListener=object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.sendTyping(binding.viewChatBtm.edtMsg.trim())
        }

        override fun afterTextChanged(s: Editable?) {
            if (s.isNullOrEmpty())
                binding.viewChatBtm.imageStreography.show()
            else
                binding.viewChatBtm.imageStreography.gone()
        }
    }

    override fun onItemClicked(v: View, position: Int) {


    }

    override fun onResume() {
        viewModel.setOnline(true)
        preference.setCurrentGroup(group.id)
        viewModel.setSeenAllMessage()
        viewModel.sendCachedMesssages()
        Utils.removeNotification(requireContext())
        super.onResume()
    }


    override fun onStop() {
        super.onStop()
        preference.clearCurrentGroup()
        viewModel.setOnline(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        Utils.closeKeyBoard(requireActivity())
    }

}