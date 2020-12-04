package com.gowtham.letschat.fragments.single_chat_home

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gowtham.letschat.R
import com.gowtham.letschat.core.ChatHandler
import com.gowtham.letschat.core.ChatUserProfileListener
import com.gowtham.letschat.core.GroupChatHandler
import com.gowtham.letschat.databinding.FSingleChatHomeBinding
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.data.ChatUserWithMessages
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.ui.activities.SharedViewModel
import com.gowtham.letschat.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class FSingleChatHome : Fragment(),ItemClickListener {

    @Inject
    lateinit var preference: MPreference

    @Inject
    lateinit var chatUserDao: ChatUserDao

    @Inject
    lateinit var messageDao: MessageDao

    private lateinit var activity: Activity

    private lateinit var profile: UserProfile

    private var chatList = mutableListOf<ChatUserWithMessages>()

    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private lateinit var binding: FSingleChatHomeBinding

    private val viewModel: SingleChatHomeViewModel by viewModels()

    private val adChat: AdSingleChatHome by lazy {
        AdSingleChatHome(requireContext())
    }

    @Inject
    lateinit var chatHandler: ChatHandler

    @Inject
    lateinit var groupChatHandler: GroupChatHandler

    @Inject
    lateinit var chatUsersListener: ChatUserProfileListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FSingleChatHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity = requireActivity()
        binding.lifecycleOwner = viewLifecycleOwner
        chatHandler.initHandler()
        groupChatHandler.initHandler()
        chatUsersListener.initListener()
        profile = preference.getUserProfile()!!
        setDataInView()
        subScribeObservers()
    }

    private fun subScribeObservers() {
        lifecycleScope.launch {
            viewModel.getChatUsers().collect { list ->
                updateList(list)
            }
        }

        sharedViewModel.getState().observe(viewLifecycleOwner,{state->
            if (state is ScreenState.IdleState){
                CoroutineScope(Dispatchers.IO).launch {
                    updateList(viewModel.getChatUsersAsList())
                }
            }
        })
        sharedViewModel.lastQuery.observe(viewLifecycleOwner,{
            if (sharedViewModel.getState().value is ScreenState.SearchState)
                adChat.filter(it)
        })
    }

    private suspend fun updateList(list: List<ChatUserWithMessages>) {
        withContext(Dispatchers.Main){
            val filteredList = list.filter { it.messages.isNotEmpty() }
            if (filteredList.isNotEmpty()) {
                binding.imageEmpty.gone()
                chatList = filteredList as MutableList<ChatUserWithMessages>
                //sort by recent message
                chatList = filteredList.sortedByDescending { it.messages.last().createdAt }
                    .toMutableList()
                AdSingleChatHome.allChatList=chatList
                adChat.submitList(chatList)
                if(sharedViewModel.getState().value is ScreenState.SearchState)
                    adChat.filter(sharedViewModel.lastQuery.value.toString())
            }else
                binding.imageEmpty.show()
        }
    }

    private fun setDataInView() {
        binding.listChat.itemAnimator = null
        binding.listChat.adapter = adChat
        AdSingleChatHome.itemClickListener = this
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Utils.isPermissionOk(*grantResults)){
            if (findNavController().isValidDestination(R.id.FSingleChatHome))
                findNavController().navigate(R.id.action_FSingleChatHome_to_FContacts)
        }
        else
            activity.toast("Permission is needed!")
    }

    override fun onItemClicked(v: View, position: Int) {
        sharedViewModel.setState(ScreenState.IdleState)
        val chatUser=adChat.currentList[position]
        preference.setCurrentUser(chatUser.user.id)
        val action= FSingleChatHomeDirections.actionFSingleChatToFChat(chatUser.user)
        findNavController().navigate(action)
    }
}