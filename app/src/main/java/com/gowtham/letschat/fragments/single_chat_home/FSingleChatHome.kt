package com.gowtham.letschat.fragments.single_chat_home

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class FSingleChatHome : Fragment(),ItemClickListener {

    @Inject
    lateinit var chatUsersListener: ChatUserProfileListener

    @Inject
    lateinit var preference: MPreference

    @Inject
    lateinit var chatUserDao: ChatUserDao

    @Inject
    lateinit var messageDao: MessageDao

    private lateinit var activity: Activity

    private lateinit var profile: UserProfile

    private var chatList = mutableListOf<ChatUserWithMessages>()

    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var binding: FSingleChatHomeBinding

    private val viewModel: SingleChatHomeViewModel by viewModels()

    private val adChat: AdSingleChatHome by lazy {
        AdSingleChatHome(requireContext())
    }

    @Inject
    lateinit var chatHandler: ChatHandler

    @Inject
    lateinit var groupChatHandler: GroupChatHandler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = FSingleChatHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity = requireActivity()
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        profile = preference.getUserProfile()!!
        setDataInView()

        lifecycleScope.launch {
            viewModel.getChatUsers().collect { list ->
                val filteredList = list.filter { it.messages.isNotEmpty() }
                if (filteredList.isNotEmpty()) {
                    binding.imageEmpty.gone()
                    chatList = filteredList as MutableList<ChatUserWithMessages>
                    //sort by recent message
                    chatList = filteredList.sortedByDescending { it.messages.last().createdAt }
                        .toMutableList()
                    adChat.submitList(chatList)
                    Timber.v("Userss ${chatList.first().user.unRead}")
                }else
                    binding.imageEmpty.show()
            }
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
        preference.setCurrentUser(chatList[position].user.id)
        val action= FSingleChatHomeDirections.actionFSingleChatToFChat(chatList[position].user)
        findNavController().navigate(action)
    }
}