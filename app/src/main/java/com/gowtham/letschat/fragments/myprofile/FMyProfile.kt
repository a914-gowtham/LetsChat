package com.gowtham.letschat.fragments.myprofile

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.canhub.cropper.CropImage
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.AlertLogoutBinding
import com.gowtham.letschat.databinding.FMyProfileBinding
import com.gowtham.letschat.db.ChatUserDatabase
import com.gowtham.letschat.utils.*
import com.gowtham.letschat.views.CustomProgressView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class FMyProfile : Fragment(R.layout.f_my_profile) {

    private lateinit var binding: FMyProfileBinding

    @Inject
    lateinit var preferenec: MPreference

    @Inject
    lateinit var db: ChatUserDatabase

    private lateinit var dialog: Dialog

    private val viewModel: FMyProfileViewModel by viewModels()

    private lateinit var context: Activity

    private var progressView: CustomProgressView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FMyProfileBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context = requireActivity()
        progressView = CustomProgressView(context)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.imageProfile.setOnClickListener {
             ImageUtils.askPermission(this)
        }
        binding.btnSaveChanges.setOnClickListener {
            val newName = viewModel.userName.value
            val about = viewModel.about.value
            val image=viewModel.imageUrl.value
            when {
                viewModel.isUploading.value!! -> context.toast("Profile picture is uploading!")
                newName.isNullOrBlank() -> context.toast("User name can't be empty!")
                else -> {
                    context.window.decorView.clearFocus()
                    viewModel.saveChanges(newName,about ?: "" ,image ?: "")
                }
            }
        }
        binding.btnLogout.setOnClickListener {
            dialog.show()
        }
        initDialog()
        subscribeObservers()
    }

    private fun subscribeObservers() {
        viewModel.profileUpdateState.observe(viewLifecycleOwner, {
            if (it is LoadState.OnLoading) {
                progressView?.show()
            } else
                progressView?.dismiss()
        })
    }

    private fun initDialog() {
        try {
            dialog = Dialog(requireContext())
            val layoutBinder = AlertLogoutBinding.inflate(layoutInflater)
            dialog.setContentView(layoutBinder.root)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutBinder.txtOk.setOnClickListener {
                dialog.dismiss()
                UserUtils.logOut(requireActivity(), preferenec, db)
            }
            layoutBinder.txtCancel.setOnClickListener {
                dialog.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ImageUtils.onImagePerResult(this, *grantResults)
    }
}