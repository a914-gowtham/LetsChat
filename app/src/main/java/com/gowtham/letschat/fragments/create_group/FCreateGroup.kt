package com.gowtham.letschat.fragments.create_group

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.canhub.cropper.CropImage
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.FCreateGroupBinding
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.fragments.add_group_members.AdAddMembers
import com.gowtham.letschat.utils.*
import com.gowtham.letschat.views.CustomProgressView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FCreateGroup : Fragment() {

    @Inject
    lateinit var chatUserDao: ChatUserDao

    @Inject
    lateinit var preference: MPreference

    private val viewModel: CreateGroupViewModel by viewModels()

    private lateinit var binding: FCreateGroupBinding

    val args by navArgs<FCreateGroupArgs>()

    private lateinit var memberList: List<ChatUser>

    private var progressView: CustomProgressView?=null

    private val adMembers: AdAddMembers by lazy {
        AdAddMembers(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding=FCreateGroupBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner=viewLifecycleOwner
        binding.viewmodel=viewModel
        progressView= CustomProgressView(requireContext())
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.imageAddImage.setOnClickListener {
            ImageUtils.askPermission(this)
        }
        binding.fab.setOnClickListener {
          validate()
        }
        setDataInView()
        subscribeObservers()
    }

    private fun subscribeObservers() {
        viewModel.groupCreateStatus.observe(viewLifecycleOwner,{
            when (it) {
                is LoadState.OnSuccess -> {
                    if (findNavController().isValidDestination(R.id.FCreateGroup)) {
                        progressView?.dismiss()
                        val group=it.data as Group
                        preference.setCurrentGroup(group.id)
                        val action=FCreateGroupDirections.actionFCreateGroupToFGroupChat(group)
                        findNavController().navigate(action)
                    }
                }
                is LoadState.OnFailure -> {
                    progressView?.dismiss()
                }
                is LoadState.OnLoading -> {
                    progressView?.show()
                }
            }
        })
    }

    private fun validate() {
        val groupName=viewModel.groupName.value.toString().trim()
        if (groupName.isNotEmpty() && !viewModel.progressProPic.value!!)
            viewModel.createGroup(memberList as ArrayList<ChatUser>)
    }

    private fun setDataInView() {
        binding.edtGroupName.requestFocus()
        Utils.showSoftKeyboard(requireActivity(),binding.edtGroupName)
        memberList=args.memberList.toList().map {
            it.isSelected=false
            it
        }
        val memberCount=memberList.size
        binding.memberCount=if(memberCount==1) "$memberCount member" else "$memberCount members"
        binding.listMembers.adapter = adMembers
        adMembers.addRestorePolicy()
        adMembers.submitList(memberList)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ImageUtils.onImagePerResult(this, *grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
            onCropResult(data)
        else
            ImageUtils.cropImage(requireActivity(), data)
    }

    private fun onCropResult(data: Intent?) {
        try {
            val imagePath: Uri? = ImageUtils.getCroppedImage(data)
            imagePath?.let {
                viewModel.uploadProfileImage(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressView?.dismissIfShowing()
        Utils.closeKeyBoard(requireActivity())
    }

}