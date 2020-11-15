package com.gowtham.letschat.fragments.group_chat_home

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.FGroupChatHomeBinding
import com.gowtham.letschat.db.data.GroupWithMessages
import com.gowtham.letschat.ui.activities.SharedViewModel
import com.gowtham.letschat.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class FGroupChatHome : Fragment(),ItemClickListener{

    private val viewModel: GroupChatHomeViewModel by viewModels()

    private lateinit var binding: FGroupChatHomeBinding

    private val sharedViewModel by activityViewModels<SharedViewModel>()

    @Inject
    lateinit var preference: MPreference

    private lateinit var activity: Activity

    private val groups= mutableListOf<GroupWithMessages>()

    private val adGroupHome by lazy {
        AdGroupChatHome(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = FGroupChatHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity=requireActivity()
        binding.lifecycleOwner = viewLifecycleOwner
        setDataInView()
        subscribeObservers()
    }

    private fun subscribeObservers() {
        lifecycleScope.launch {
            viewModel.getGroupMessages().collect { groupWithmsgs ->
                updateList(groupWithmsgs)
            }
        }

        sharedViewModel.getState().observe(viewLifecycleOwner,{state->
            if (state is ScreenState.IdleState){
                CoroutineScope(Dispatchers.IO).launch {
                    updateList(viewModel.getGroupMessagesAsList())
                }
            }
        })

        sharedViewModel.lastQuery.observe(viewLifecycleOwner,{
            if (sharedViewModel.getState().value is ScreenState.SearchState)
                adGroupHome.filter(it)
        })
    }

    private suspend fun updateList(groupWithmsgs: List<GroupWithMessages>) {
        withContext(Dispatchers.Main){
            if (!groupWithmsgs.isNullOrEmpty()) {
                val list1=  groupWithmsgs.filter { it.messages.isEmpty() }
                    .sortedByDescending { it.group.createdAt }.toMutableList()
                val groupHasMsgsList=groupWithmsgs.filter { it.messages.isNotEmpty() }.
                sortedBy { it.messages.last().createdAt }

                for (a in groupHasMsgsList)
                    list1.add(0,a)

                adGroupHome.submitList(list1)
                AdGroupChatHome.allList=list1
                groups.clear()
                groups.addAll(list1)
                if(sharedViewModel.getState().value is ScreenState.SearchState)
                    adGroupHome.filter(sharedViewModel.lastQuery.value.toString())
            }else
                binding.imageEmpty.show()
        }
    }

    private fun setDataInView() {
        binding.listGroup.adapter = adGroupHome
        binding.listGroup.itemAnimator = null
        AdGroupChatHome.itemClickListener=this
        adGroupHome.addRestorePolicy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Utils.isPermissionOk(*grantResults) &&
            findNavController().isValidDestination(R.id.FGroupChatHome)){
            findNavController().navigate(R.id.action_FGroupChatHome_to_FAddGroupMembers)
        }
        else
            activity.toast("Permission is needed!")
    }

    override fun onItemClicked(v: View, position: Int) {
        sharedViewModel.setState(ScreenState.IdleState)
        val group = adGroupHome.currentList[position].group
        preference.setCurrentGroup(group.id)
        val action = FGroupChatHomeDirections.actionFGroupChatHomeToFGroupChat(group)
        findNavController().navigate(action)
    }
}