package com.gowtham.letschat.fragments.contacts

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.FContactsBinding
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.utils.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class FContacts @Inject constructor(private val preference: MPreference) : Fragment(), ItemClickListener {
 
    private lateinit var binding: FContactsBinding

    private lateinit var context: Activity

    private lateinit var searchView: SearchView

    private lateinit var searchItem: MenuItem

    private lateinit var menuRefresh: MenuItem

    private val viewModel: ContactsViewModel by viewModels()

    private var contactList = ArrayList<ChatUser>()

    private lateinit var adContact: AdContact

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FContactsBinding.inflate(layoutInflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context = requireActivity()
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewmodel = viewModel
        setToolbar()
        setDataInView()
        subscribeObservers()
    }

    private fun subscribeObservers() {
        viewModel.getContacts().observe(viewLifecycleOwner, { contacts->
            LogMessage.v("Size ${contacts.size}")
            val allContacts=contacts.filter { it.locallySaved }
            if (allContacts.isEmpty() && viewModel.queryState.value == null)
                viewModel.startQuery()
            else {
                viewModel.setContactCount(allContacts.size)
                contactList.clear()
                contactList= allContacts as ArrayList<ChatUser>
                adContact = AdContact(requireContext(), contactList)
                binding.listContact.adapter = adContact
                if(searchItem.isActionViewExpanded)
                    adContact.filter(searchView.query.toString())
            }
        })

        viewModel.queryState.observe(viewLifecycleOwner,{
            searchItem.isEnabled = it !is LoadState.OnLoading
            menuRefresh.isEnabled = it !is LoadState.OnLoading
        })
    }

    private fun setDataInView() {
        try {
            adContact = AdContact(requireContext(), contactList)
            binding.listContact.adapter = adContact
            AdContact.itemClickListener = this
            adContact.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setToolbar() {
        try {
            binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            binding.toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
            binding.toolbar.inflateMenu(R.menu.menu_contacts)
            searchItem = binding.toolbar.menu.findItem(R.id.action_search)
            menuRefresh = binding.toolbar.menu.findItem(R.id.action_refresh)
            menuRefresh.setOnMenuItemClickListener {
                viewModel.startQuery()
                true
            }
            searchView = searchItem.actionView as SearchView
            searchView.apply {
                maxWidth = Integer.MAX_VALUE
                queryHint = getString(R.string.txt_search)
            }

            searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    menuRefresh.isVisible = false
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    menuRefresh.isVisible = true
                    return true
                }
            })

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    adContact.filter(newText.toString())
                    return true
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onItemClicked(v: View, position: Int) {
        viewModel.setUnReadCountZero(contactList[position])
        preference.setCurrentUser(contactList[position].user.uId!!)
        val action = FContactsDirections.actionFContactsToChat(
            contactList[position]
        )
        findNavController().navigate(action)
    }
}
