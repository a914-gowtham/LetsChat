package com.gowtham.letschat.fragments.group_chat

import android.Manifest
import android.animation.Animator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.canhub.cropper.CropImage
import com.gowtham.letschat.databinding.FGroupChatBinding
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.db.data.*
import com.gowtham.letschat.fragments.FAttachment
import com.gowtham.letschat.models.MyImage
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.*
import com.gowtham.letschat.utils.Events.EventAudioMsg
import com.gowtham.letschat.views.CustomEditText
import com.stfalcon.imageviewer.StfalconImageViewer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class FGroupChat : Fragment(), ItemClickListener, CustomEditText.KeyBoardInputCallbackListener {

    @Inject
    lateinit var groupDao: GroupDao

    @Inject
    lateinit var preference: MPreference

    private val viewModel: GroupChatViewModel by viewModels()

    private lateinit var binding: FGroupChatBinding

    val args by navArgs<FGroupChatArgs>()

    lateinit var group: Group

    private var messageList = mutableListOf<GroupMessage>()

    private lateinit var manager: LinearLayoutManager

    private lateinit var localUserId: String

    private lateinit var fromUser: UserProfile

    private var lastAudioFile=""

    private var msgPostponed=false

    var isRecording = false //whether is recoding now or not

    private var recordStart = 0L

    private var recorder: MediaRecorder? = null

    private val REQ_AUDIO_PERMISSION=29

    private var recordDuration = 0L

    private val adChat: AdGroupChat by lazy {
        AdGroupChat(requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FGroupChatBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewmodel = viewModel
        group = args.group
        binding.group = group
        setViewListeners()
        binding.viewChatBtm.edtMsg.setKeyBoardInputCallbackListener(this)
        UserUtils.setUnReadCountGroup(groupDao, group)
        setDataInView()
        subscribeObservers()

        lifecycleScope.launch {
            viewModel.getGroupMessages(group.id).collect { message ->
                if(message.isEmpty())
                    return@collect
                messageList = message as MutableList<GroupMessage>
                if(AdGroupChat.isPlaying()){
                    msgPostponed=true
                    return@collect
                }
                AdGroupChat.messageList = messageList
                adChat.submitList(messageList)
                Timber.v("Message list ${messageList.last()}")
                //scroll to last items in recycler (recent messages)
                if (messageList.isNotEmpty()) {
                    if (viewModel.getCanScroll())  //scroll only if new message arrived
                        binding.listMessage.smoothScrollToPos(messageList.lastIndex)
                    else
                        viewModel.canScroll(true)
                }
            }
        }
    }

    private fun setViewListeners() {
        binding.viewChatBtm.lottieSend.setOnClickListener {
            sendMessage()
        }
        binding.viewChatHeader.viewBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.viewChatBtm.imgRecord.setOnClickListener {
            AdGroupChat.stopPlaying()
            if(Utils.checkPermission(this, Manifest.permission.RECORD_AUDIO,reqCode = REQ_AUDIO_PERMISSION))
                startRecording()
        }
        binding.viewChatBtm.imageAdd.setOnClickListener {
            val fragment= FAttachment.newInstance(Bundle())
            fragment.show(childFragmentManager,"")
        }
        binding.lottieVoice.setOnClickListener {
            if (isRecording){
                stopRecording()
                val duration=(recordDuration/1000).toInt()
                if (duration<=1) {
                    requireContext().toast("Nothing is recorded!")
                    return@setOnClickListener
                }
                val msg=createMessage()
                msg.type="audio"
                msg.audioMessage= AudioMessage(lastAudioFile,duration)
                viewModel.uploadToCloud(msg,lastAudioFile)
            }
        }
    }

    private fun sendMessage() {
        val msg = binding.viewChatBtm.edtMsg.text?.trim().toString()
        if (msg.isEmpty())
            return
        binding.viewChatBtm.lottieSend.playAnimation()
        val messageData = TextMessage(msg)
        val message = createMessage()
        message.textMessage = messageData
        viewModel.sendMessage(message)
        binding.viewChatBtm.edtMsg.setText("")
    }

    private fun createMessage(): GroupMessage {
        val toUsers = group.members?.map { it.id } as ArrayList
        val groupSize = group.members!!.size
        val statusList = ArrayList<Int>()
        val deliveryTimeList = ArrayList<Long>()
        for (index in 0 until groupSize) {
            statusList.add(0)
            deliveryTimeList.add(0L)
        }
        return GroupMessage(
            System.currentTimeMillis(), group.id, from = localUserId,
            to = toUsers, fromUser.userName, fromUser.image, statusList, deliveryTimeList,
            deliveryTimeList
        )
    }

    private fun subscribeObservers() {
        viewModel.getChatUsers().observe(viewLifecycleOwner, { chatUsers ->
            AdGroupChat.chatUserList = chatUsers.toMutableList()
        })

        viewModel.typingUsers.observe(viewLifecycleOwner, { typingUser ->
            if (typingUser.isEmpty())
                BindingAdapters.setMemberNames(binding.viewChatHeader.txtMembers, group)
            else
                binding.viewChatHeader.txtMembers.text = typingUser
        })
    }

    private fun setDataInView() {
        fromUser = preference.getUserProfile()!!
        localUserId = fromUser.uId!!
        manager = LinearLayoutManager(context)
        binding.listMessage.apply {
            manager.stackFromEnd = true
            layoutManager = manager
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            itemAnimator = null
        }
        binding.listMessage.adapter = adChat
        adChat.addRestorePolicy()
        viewModel.setGroup(group)
        binding.viewChatBtm.edtMsg.addTextChangedListener(msgTxtChangeListener)
        binding.viewChatBtm.lottieSend.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator?) {


            }

            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                super.onAnimationEnd(animation, isReverse)
            }

            override fun onAnimationEnd(p0: Animator?) {
                if (Utils.edtValue(binding.viewChatBtm.edtMsg).isEmpty()) {
                    binding.viewChatBtm.imgRecord.show()
                    binding.viewChatBtm.lottieSend.gone()
                }
            }

            override fun onAnimationCancel(p0: Animator?) {
            }

            override fun onAnimationRepeat(p0: Animator?) {


            }
        })

        binding.lottieVoice.addAnimatorListener(object : Animator.AnimatorListener{
            override fun onAnimationStart(p0: Animator?) {


            }

            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                super.onAnimationEnd(animation, isReverse)
            }

            override fun onAnimationEnd(p0: Animator?) {
                binding.viewChatBtm.imgRecord.show()
                binding.lottieVoice.gone()
            }

            override fun onAnimationCancel(p0: Animator?) {
            }

            override fun onAnimationRepeat(p0: Animator?) {


            }
        })
    }

    private val msgTxtChangeListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.sendTyping(binding.viewChatBtm.edtMsg.trim())
            if(binding.viewChatBtm.lottieSend.isAnimating)
                return
            if(s.isNullOrBlank()) {
                binding.viewChatBtm.imgRecord.show()
                binding.viewChatBtm.lottieSend.hide()
            }
            else{
                binding.viewChatBtm.lottieSend.show()
                binding.viewChatBtm.imgRecord.hide()
            }
        }

        override fun afterTextChanged(s: Editable?) {
        }
    }

    override fun onItemClicked(v: View, position: Int) {
        binding.fullSizeImageView.show()
        StfalconImageViewer.Builder(
            context,
            listOf(MyImage(messageList.get(position).imageMessage?.uri!!))) { imageView, myImage ->
            ImageUtils.loadGalleryImage(myImage.url,imageView)
        }
            .withDismissListener { binding.fullSizeImageView.visibility = View.GONE }
            .show()
    }

    override fun onResume() {
        preference.setCurrentGroup(group.id)
        viewModel.sendCachedTxtMesssages()
        Utils.removeNotification(requireContext())
        super.onResume()
    }

    override fun onCommitContent(
        inputContentInfo: InputContentInfoCompat?,
        flags: Int,
        opts: Bundle?) {
        val imageMsg = createMessage()
        val image = ImageMessage("${inputContentInfo?.contentUri}")
        image.imageType = if (image.uri.toString().endsWith(".png")) "sticker" else "gif"
        imageMsg.apply {
            type = "image"
            imageMessage = image
        }
        viewModel.uploadToCloud(imageMsg,image.toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
            onCropResult(data)
        else
            ImageUtils.cropImage(requireActivity(), data, true)
    }

    private fun onCropResult(data: Intent?) {
        try {
            val imagePath: Uri? = ImageUtils.getCroppedImage(data)
            if (imagePath!=null){
                val message=createMessage()
                message.type="image"
                message.imageMessage=ImageMessage(imagePath.toString())
                viewModel.uploadToCloud(message,imagePath.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==REQ_AUDIO_PERMISSION){
            if (Utils.isPermissionOk(*grantResults))
                startRecording()
            else
                requireActivity().toast("Audio permission is needed!")
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAttachmentItemClicked(event: BottomSheetEvent) {
        when (event.position) {
            0 -> {
                ImageUtils.takePhoto(requireActivity())
            }
            1 -> {
                ImageUtils.chooseGallery(requireActivity())
            }
            2 -> {
                //create intent for gallery video
            }
            3 -> {
                //create intent for camera video
            }
        }
    }

    private fun startRecording() {
        binding.lottieVoice.show()
        binding.lottieVoice.playAnimation()
        binding.viewChatBtm.edtMsg.apply {
            isEnabled=false
            hint="Recording..."
        }
        onAudioEvent(EventAudioMsg(true))
        //name of the file where record will be stored
        lastAudioFile=
            "${requireActivity().externalCacheDir?.absolutePath}/audiorecord${System.currentTimeMillis()}.mp3"
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            setOutputFile(lastAudioFile)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
            } catch (e: IOException) {
                println("ChatFragment.startRecording${e.message}")
            }
            start()
            isRecording=true
            recordStart = Date().time
        }
        Handler(Looper.getMainLooper()).postDelayed({
            binding.lottieVoice.pauseAnimation()
        },800)
    }


    private fun stopRecording() {
        onAudioEvent(EventAudioMsg(false))
        binding.viewChatBtm.edtMsg.apply {
            isEnabled=true
            hint="Type Something..."
        }
        Handler(Looper.getMainLooper()).postDelayed({
            binding.lottieVoice.resumeAnimation()
        },200)
        recorder?.apply {
            stop()
            release()
            recorder = null
        }
        isRecording=false
        recordDuration = Date().time - recordStart
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecording()
        AdGroupChat.stopPlaying()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onAudioEvent(audioEvent: EventAudioMsg){
        if (audioEvent.isPlaying){
            //lock current orientation
            val currentOrientation=requireActivity().resources.configuration.orientation
            requireActivity().requestedOrientation = currentOrientation
        }else {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (msgPostponed){
                //refresh list
                AdGroupChat.messageList = messageList
                adChat.submitList(messageList)
                msgPostponed=false
            }
        }
    }

    override fun onStop() {
        super.onStop()
        preference.clearCurrentGroup()
    }

    override fun onDestroy() {
        super.onDestroy()
        Utils.closeKeyBoard(requireActivity())
    }

}