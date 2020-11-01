package com.gowtham.letschat.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.*
import com.gowtham.letschat.db.ChatUserDatabase
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.db.daos.GroupMessageDao
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.utils.MPreference
import com.gowtham.letschat.utils.UserUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class FMyProfile : Fragment(R.layout.f_my_profile){

    private lateinit var binding: FMyProfileBinding

    @Inject
    lateinit var userDao: ChatUserDao

    @Inject
    lateinit var messageDao: MessageDao

    @Inject
    lateinit var groupDao: GroupDao

    @Inject
    lateinit var groupMsgDao: GroupMessageDao

    @Inject
    lateinit var preferenec: MPreference

    @Inject
    lateinit var db: ChatUserDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.f_my_profile, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner


        binding.button.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                UserUtils.logOut(requireActivity(),preferenec,db)
            }
        }
    }


}