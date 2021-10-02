package com.gowtham.letschat.fragments.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.canhub.cropper.CropImage
import com.google.firebase.firestore.CollectionReference
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.FProfileBinding
import com.gowtham.letschat.databinding.FVerifyBinding
import com.gowtham.letschat.models.UserStatus
import com.gowtham.letschat.ui.activities.MainActivity
import com.gowtham.letschat.utils.*
import com.gowtham.letschat.views.CustomProgressView
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class FProfile : Fragment() {

    private lateinit var binding: FProfileBinding

    private lateinit var context: Activity

    @Inject
    lateinit var preference: MPreference

    @Inject
    lateinit var userCollection: CollectionReference

    private var progressView: CustomProgressView? = null

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = FProfileBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context = requireActivity()
        UserUtils.updatePushToken(context,userCollection,true)
        EventBus.getDefault().post(UserStatus())
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewmodel = viewModel
        progressView = CustomProgressView(context)
        binding.imgProPic.setOnClickListener { ImageUtils.askPermission(this) }
        binding.fab.setOnClickListener { validate() }
        subscribeObservers()
    }

    private fun subscribeObservers() {
        viewModel.progressProPic.observe(viewLifecycleOwner, { uploaded ->
            binding.progressPro.toggle(uploaded)
        })

        viewModel.profileUpdateState.observe(viewLifecycleOwner, {
            when (it) {
                is LoadState.OnSuccess -> {
                    if (findNavController().isValidDestination(R.id.FProfile)) {
                        progressView?.dismiss()
                        findNavController().navigate(R.id.action_FProfile_to_FSingleChatHome)
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

        viewModel.checkUserNameState.observe(viewLifecycleOwner,{
            when (it) {
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
        val name = viewModel.name.value
        if (!name.isNullOrEmpty() && name.length > 1 && !viewModel.progressProPic.value!!)
            viewModel.storeProfileData()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
            onCropResult(data)
        else
            ImageUtils.cropImage(context, data, true)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ImageUtils.onImagePerResult(this, *grantResults)
    }

    override fun onDestroy() {
        try {
            progressView?.dismissIfShowing()
            super.onDestroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}