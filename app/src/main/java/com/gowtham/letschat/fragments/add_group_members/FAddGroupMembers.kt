package com.gowtham.letschat.fragments.add_group_members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.FAddGroupMembersBinding
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.utils.*
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FAddGroupMembers : Fragment(), ItemClickListener {

    private lateinit var binding: FAddGroupMembersBinding

    @Inject
    lateinit var preference: MPreference

    private lateinit var searchView: SearchView

    private var contactList = ArrayList<ChatUser>()

    private val adContact: AdAddMembers by lazy {
        AdAddMembers(requireContext())
    }

    private val adChip: AdChip by lazy {
        AdChip(requireContext())
    }

    private val viewModel: AddGroupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FAddGroupMembersBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.v("onViewCreated")
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.fab.setOnClickListener {
            val addedContacts = AdChip.allAddedContacts
            if (addedContacts.isNotEmpty()) {
                val action = FAddGroupMembersDirections.actionFAddGroupMembersToFCreateGroup(
                    addedContacts.toTypedArray()
                )
                findNavController().navigate(action)
            }
        }
        setToolbar()
        setDataInView()
        subscribeObservers()
    }

    private fun setToolbar() {
        binding.toolbar.inflateMenu(R.menu.menu_search)
        val searchItem: MenuItem? = binding.toolbar.menu.findItem(R.id.action_search)
        searchView = searchItem?.actionView as SearchView
        searchView.apply {
            maxWidth = Integer.MAX_VALUE
            queryHint = getString(R.string.txt_search)
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adContact.filter(newText.toString())
                return true
            }
        })
    }

    private fun subscribeObservers() {
        viewModel.getChatList().observe(viewLifecycleOwner, { contacts ->
            val allContacts = contacts.filter { it.locallySaved }
            if (allContacts.isNotEmpty()) {
                if (viewModel.isFirstCall) {
                    viewModel.setContactList(allContacts)
                    viewModel.isFirstCall=false
                }
                Timber.v("allContacts ->${viewModel.getContactList().first().localName}")
                contactList.clear()
                contactList.addAll(viewModel.getContactList())
                AdAddMembers.allContacts = contactList
                adContact.submitList(contactList)
                if (!searchView.isIconified)
                    adContact.filter(searchView.query.toString())
            }
        })

        viewModel.getChipList().observe(viewLifecycleOwner, { addedList ->
            AdChip.allAddedContacts = addedList
            adChip.submitList(addedList.toList())
            adChip.notifyDataSetChanged()
            if (addedList.isEmpty()) {
                binding.txtEmptyMembers.show()
                binding.fab.hide()} else {
                binding.txtEmptyMembers.hide()
                binding.fab.show()
                binding.listChip.post {
                    binding.listChip.smoothScrollToPosition(addedList.lastIndex)
                }
            }
        })

        viewModel.queryState.observe(viewLifecycleOwner, {
            searchView.isEnabled = it !is LoadState.OnLoading
            when (it) {
                is LoadState.OnSuccess -> {
                    val emptyList = it.data as ArrayList<*>
                    if (emptyList.isEmpty()) {
                        binding.viewEmpty.show()
                        binding.progress.hide()
                        binding.viewEmpty.playAnimation()
                    }else{
                        binding.viewHolder.show()
                        binding.progress.hide()
                    }
                }
                is LoadState.OnFailure -> {
                    binding.viewHolder.hide()
                    binding.progress.hide()
                    binding.viewEmpty.playAnimation()
                }
                is LoadState.OnLoading -> {
                    binding.viewEmpty.hide()
                    binding.viewHolder.hide()
                    binding.progress.show()
                }
            }
        })
    }

    private fun setDataInView() {
        binding.listContact.adapter = adContact
        binding.listChip.adapter = adChip
        AdAddMembers.listener = this
        AdChip.listener = chipListener
        adContact.addRestorePolicy()
        adChip.addRestorePolicy()
    }

    override fun onItemClicked(v: View, position: Int) {
        val currentList = ArrayList(adContact.currentList)
        val user = currentList[position]
        user.apply {
            isSelected = !isSelected
        }
        currentList.set(position, user)
        adContact.submitList(currentList)
        adContact.notifyItemChanged(position)
        val allContact = AdAddMembers.allContacts
        val user1 = allContact.find { it.id == user.id }
        val index = allContact.indexOf(user1)
        allContact.set(index, user1!!)
        viewModel.setContactList(allContact) //update in allContacts list

        val chipList = AdChip.allAddedContacts
        val contains = chipList.find { it.id == user.id }
        if (contains == null)
            chipList.add(user)
        else
            chipList.set(chipList.indexOf(contains), user)
        viewModel.setChipList(chipList)  //update chip list
    }

    private val chipListener: ItemClickListener = object : ItemClickListener {
        override fun onItemClicked(v: View, position: Int) {
            val added = AdChip.allAddedContacts
            var index: Int?
            val clickedUser = added.get(position)

            val currentList = ArrayList(adContact.currentList)
            val user = currentList.find { it.id == clickedUser.id }
            if (user != null) {  //update in current list
                index = currentList.indexOf(user)
                user.isSelected = false
                currentList.set(index, user)
                adContact.submitList(currentList)
                adContact.notifyItemChanged(index)
            }

            val allUsers = AdAddMembers.allContacts //update allContactList
            val user1 = allUsers.find { it.id == clickedUser.id }
            val indexAllList = allUsers.indexOf(user1)
            user1?.isSelected = false
            allUsers.set(indexAllList, user1!!)
            viewModel.setContactList(allUsers)

            added.removeAt(position)
            viewModel.setChipList(added)
            if (!searchView.isIconified)  //remove from chip list
                adContact.filter(searchView.query.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        val chipList = AdChip.allAddedContacts
        val allUsers = AdAddMembers.allContacts
        for (user in allUsers)
            user.isSelected = chipList.contains(user)
        viewModel.setContactList(allUsers)
    }
}