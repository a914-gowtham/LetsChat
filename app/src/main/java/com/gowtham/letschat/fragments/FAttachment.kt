package com.gowtham.letschat.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gowtham.letschat.databinding.FAttachmentBinding
import com.gowtham.letschat.databinding.FImageSrcSheetBinding
import com.gowtham.letschat.utils.BottomSheetEvent
import org.greenrobot.eventbus.EventBus

class FAttachment  : BottomSheetDialogFragment() {

    private lateinit var binding: FAttachmentBinding

    companion object{
        fun newInstance(bundle : Bundle) : FAttachment{
            val fragment = FAttachment()
            fragment.arguments=bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FAttachmentBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgCamera.setOnClickListener {
            EventBus.getDefault().post(BottomSheetEvent(0))
            dismiss()
        }

        binding.imgGallery.setOnClickListener {
            EventBus.getDefault().post(BottomSheetEvent(1))
            dismiss()
        }

        binding.videoGallery.setOnClickListener {
            EventBus.getDefault().post(BottomSheetEvent(2))
            dismiss()
        }

        binding.videoCamera.setOnClickListener {
            EventBus.getDefault().post(BottomSheetEvent(3))
            dismiss()
        }

    }
}