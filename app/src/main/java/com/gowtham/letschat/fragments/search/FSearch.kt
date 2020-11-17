package com.gowtham.letschat.fragments.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.FSearchBinding
import com.gowtham.letschat.databinding.FSingleChatHomeBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FSearch : Fragment(R.layout.f_search){

    private lateinit var binding: FSearchBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = FSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        setDataInView()
    }

    private fun setDataInView() {


    }


}