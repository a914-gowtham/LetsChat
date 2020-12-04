package com.gowtham.letschat.fragments.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.paging.LoadState
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.FSearchBinding
import com.gowtham.letschat.ui.activities.SharedViewModel
import com.gowtham.letschat.utils.ItemClickListener
import com.gowtham.letschat.utils.ScreenState
import com.gowtham.letschat.utils.gone
import com.gowtham.letschat.utils.show
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


@AndroidEntryPoint
class FSearch : Fragment(R.layout.f_search),ItemClickListener{

    private lateinit var binding: FSearchBinding

    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private val viewModel: FSearchViewModel by viewModels()

    private val adapter: AdSearch by lazy {
        AdSearch(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
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
        binding.apply {
            listUsers.setHasFixedSize(true)
            listUsers.itemAnimator = null
            listUsers.adapter = adapter.withLoadStateHeaderAndFooter(
                header = LoadStateAdapter { adapter.retry() },
                footer = LoadStateAdapter { adapter.retry() }
            )
            buttonRetry.setOnClickListener { adapter.retry() }
        }


        adapter.addLoadStateListener { loadState ->
            binding.apply {
                progressBar.isVisible = loadState.source.refresh is LoadState.Loading
                listUsers.isVisible = loadState.source.refresh is LoadState.NotLoading
                buttonRetry.isVisible = loadState.source.refresh is LoadState.Error
                txtError.isVisible = loadState.source.refresh is LoadState.Error
                binding.viewEmpty.isVisible = loadState.source.refresh is LoadState.NotLoading
                // empty view
                if (loadState.source.refresh is LoadState.NotLoading &&
                    loadState.append.endOfPaginationReached &&
                    adapter.itemCount < 1) {
                    listUsers.isVisible = false
                    txtNoUser.isVisible = true
                    binding.viewEmpty.isVisible = false
                } else {
                    txtNoUser.isVisible = false
                }
                if(adapter.itemCount!=0)
                    binding.viewEmpty.gone()
            }
        }
    }


    private fun subscribeObservers() {
        sharedViewModel.getState().observe(viewLifecycleOwner,{state->
            if (state is ScreenState.IdleState){
                binding.txtError.gone()
                binding.txtNoUser.gone()
                binding.viewEmpty.show()
            }else{
                binding.viewEmpty.gone()
            }
        })

        viewModel.users.observe(viewLifecycleOwner) {
            adapter.submitData(viewLifecycleOwner.lifecycle, it)
        }

        sharedViewModel.lastQuery.observe(viewLifecycleOwner, {
            if (sharedViewModel.getState().value is ScreenState.SearchState) {
                if (it.isNullOrBlank()){
                    binding.txtError.gone()
                    binding.txtNoUser.gone()
                    binding.viewEmpty.show()
                }else
                  viewModel.makeQuery(it.toLowerCase(Locale.getDefault()))
            }
        })
    }

    override fun onItemClicked(v: View, position: Int) {

    }
}