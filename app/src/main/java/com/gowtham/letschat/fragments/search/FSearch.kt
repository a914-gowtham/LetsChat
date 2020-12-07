package com.gowtham.letschat.fragments.search

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
import com.gowtham.letschat.databinding.FSearchBinding
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.fragments.contacts.AdContact
import com.gowtham.letschat.fragments.single_chat_home.FSingleChatHomeDirections
import com.gowtham.letschat.ui.activities.SharedViewModel
import com.gowtham.letschat.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class FSearch : Fragment(R.layout.f_search), ItemClickListener {

    private lateinit var binding: FSearchBinding

    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private val viewModel: FSearchViewModel by viewModels()

    private val userList = arrayListOf<ChatUser>()

    @Inject
    lateinit var preference: MPreference

    private val adapter: AdContact by lazy {
        AdContact(requireContext(), userList)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        setDataInView()
        subscribeObservers()
    }

    private fun setDataInView() {
        AdContact.itemClickListener=this
        binding.apply {
            listUsers.setHasFixedSize(true)
            listUsers.itemAnimator = null
            listUsers.adapter = adapter
        }
    }


    private fun subscribeObservers() {
        sharedViewModel.getState().observe(viewLifecycleOwner, { state ->
            if (state is ScreenState.IdleState) {
                //show recent list
                binding.txtNoUser.gone()
                binding.viewEmpty.show()
            } else {
                if (sharedViewModel.lastQuery.value.isNullOrBlank()) {
                    binding.viewEmpty.show()
                    binding.txtNoUser.gone()
                }
            }
        })

        lifecycleScope.launch {
            viewModel.getCachedList().collect { listData ->
                Timber.v("List data $listData")
                //can be used to show recently searched user list
            }
        }

        viewModel.getLoadState().observe(viewLifecycleOwner, { state ->
            userList.clear()
            adapter.notifyDataSetChanged()
            when (state) {
                is LoadState.OnLoading -> {
                    binding.apply {
                        txtNoUser.gone()
                        viewEmpty.gone()
                        progressBar.show()
                    }
                }
                is LoadState.OnSuccess -> {
                    binding.progressBar.gone()
                    val list = state.data as List<ChatUser>
                    if (list.isEmpty()) {
                        binding.apply {
                            txtNoUser.show()
                            viewEmpty.gone()
                        }
                    } else {
                        binding.apply {
                            txtNoUser.gone()
                            viewEmpty.gone()
                        }
                    }
                    userList.addAll(list)
                    adapter.notifyDataSetChanged()
                }
                is LoadState.OnFailure -> {
                    binding.apply {
                        progressBar.gone()
                        txtNoUser.show()
                    }
                }
            }
        })

        sharedViewModel.lastQuery.observe(viewLifecycleOwner, {
            if (sharedViewModel.getState().value is ScreenState.SearchState) {
                if (it.isBlank()) {
                    binding.apply {
                        viewEmpty.show()
                        txtNoUser.gone()
                        userList.clear()
                        userList.addAll(emptyList())
                        adapter.notifyDataSetChanged()
                    }
                } else
                    viewModel.makeQuery(it.toLowerCase(Locale.getDefault()))
            }
        })
    }

    override fun onItemClicked(v: View, position: Int) {
        val chatUser=userList[position]
        preference.setCurrentUser(chatUser.id)
        val action=FSearchDirections.actionFSearchToFSingleChat(chatUser)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        viewModel.clearCachedUser()
    }
}