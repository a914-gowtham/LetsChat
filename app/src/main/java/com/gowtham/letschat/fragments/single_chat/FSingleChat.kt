package com.gowtham.letschat.fragments.single_chat

import android.Manifest
import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
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
import com.gowtham.letschat.databinding.FSingleChatBinding
import com.gowtham.letschat.db.data.*
import com.gowtham.letschat.fragments.FAttachment
import com.gowtham.letschat.models.MyImage
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.*
import com.gowtham.letschat.utils.Events.EventAudioMsg
import com.gowtham.letschat.utils.Utils.edtValue
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
class FSingleChat : Fragment(), ItemClickListener,CustomEditText.KeyBoardInputCallbackListener {

    private lateinit var binding: FSingleChatBinding

    @Inject
    lateinit var preference: MPreference

    private lateinit var chatUser: ChatUser

    private lateinit var fromUser: UserProfile

    private lateinit var toUser: UserProfile

    private var messageList = mutableListOf<Message>()

    private val viewModel: SingleChatViewModel by viewModels()

    private lateinit var localUserId: String

    private var recorder: MediaRecorder? = null

    val args by navArgs<FSingleChatArgs>()

    private lateinit var manager: LinearLayoutManager

    private lateinit var chatUserId: String

    var isRecording = false //whether is recoding now or not

    private var recordStart = 0L

    private var recordDuration = 0L

    private val REQ_AUDIO_PERMISSION=29

    private var lastAudioFile=""

    private var msgPostponed=false

    private val adChat: AdChat by lazy {
        AdChat(requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FSingleChatBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewmodel=viewModel
        chatUser= args.chatUserProfile!!
        viewModel.setUnReadCountZero(chatUser)
        setListeners()
        if(!chatUser.locallySaved && !chatUser.isSearchedUser)
            binding.viewChatHeader.imageAddContact.show()
        viewModel.canScroll(false)
        binding.viewChatBtm.edtMsg.setKeyBoardInputCallbackListener(this)
        setDataInView()
        subscribeObservers()

        lifecycleScope.launch {
            viewModel.getMessagesByChatUserId(chatUserId).collect { mMessagesList ->
                if(mMessagesList.isEmpty())
                    return@collect
                messageList = mMessagesList as MutableList<Message>
                if(AdChat.isPlaying()){
                    msgPostponed=true
                    return@collect
                }
                AdChat.messageList = messageList
                adChat.submitList(mMessagesList)
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

    private fun setListeners() {
        binding.viewChatBtm.lottieSend.setOnClickListener {
            sendMessage()
        }
        binding.viewChatHeader.viewBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.viewChatBtm.imgRecord.setOnClickListener {
            AdChat.stopPlaying()
            if(Utils.checkPermission(this, Manifest.permission.RECORD_AUDIO,reqCode = REQ_AUDIO_PERMISSION))
                startRecording()
        }
        binding.lottieVoice.setOnClickListener {
            if (isRecording){
                stopRecording()
                val duration=(recordDuration/1000).toInt()
                if (duration<=1) {
                    requireContext().toast("Nothing is recorded!")
                    return@setOnClickListener
                }
                val msg=createMessage().apply {
                    type="audio"
                    audioMessage= AudioMessage(lastAudioFile,duration)
                    chatUsers= ArrayList()
                }
                viewModel.uploadToCloud(msg,lastAudioFile)
            }
        }
        binding.viewChatHeader.imageAddContact.setOnClickListener {
            if (Utils.askContactPermission(this))
                openSaveIntent()
        }
        binding.viewChatBtm.imageAdd.setOnClickListener {
            val fragment=FAttachment.newInstance(Bundle())
            fragment.show(childFragmentManager,"")
        }
    }

    private fun setDataInView() {
        try {
            fromUser = preference.getUserProfile()!!
            localUserId=fromUser.uId!!
            manager= LinearLayoutManager(context)
            binding.listMessage.apply {
                manager.stackFromEnd=true
                layoutManager=manager
                setHasFixedSize(true)
                isNestedScrollingEnabled=false
                itemAnimator = null
            }
            binding.listMessage.adapter = adChat
            adChat.addRestorePolicy()
            viewModel.setChatUser(chatUser)
            toUser=chatUser.user
            chatUserId=toUser.uId!!
            binding.chatUser = chatUser
            binding.viewChatBtm.edtMsg.addTextChangedListener(msgTxtChangeListener)
            binding.viewChatBtm.lottieSend.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {


                }

                override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                    super.onAnimationEnd(animation, isReverse)
                }

                override fun onAnimationEnd(p0: Animator?) {
                    if (edtValue(binding.viewChatBtm.edtMsg).isEmpty()) {
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

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun subscribeObservers() {
        //pass messages list for recycler to show
        viewModel.chatUserOnlineStatus.observe(viewLifecycleOwner, {
            Utils.setOnlineStatus(binding.viewChatHeader.txtLastSeen, it, localUserId)
        })
    }

    private fun openSaveIntent() {
        val contactIntent = Intent(ContactsContract.Intents.Insert.ACTION)
        contactIntent.type = ContactsContract.RawContacts.CONTENT_TYPE
        contactIntent
            .putExtra(ContactsContract.Intents.Insert.NAME, chatUser.user.userName)
            .putExtra(ContactsContract.Intents.Insert.PHONE, chatUser.user.mobile?.number.toString())
        startActivityForResult(contactIntent, REQ_ADD_CONTACT)
    }

    private fun sendMessage() {
        val msg = edtValue(binding.viewChatBtm.edtMsg)
        if (msg.isEmpty())
            return
        binding.viewChatBtm.lottieSend.playAnimation()
        val message = createMessage().apply {
            textMessage=TextMessage(msg)
            chatUsers= ArrayList()
        }
        viewModel.sendMessage(message)
        binding.viewChatBtm.edtMsg.setText("")
    }

    private fun createMessage(): Message {
        return Message(
            System.currentTimeMillis(),
            from = preference.getUid().toString(),
            chatUserId=chatUserId,
            to = toUser.uId!!, senderName = fromUser.userName,
            senderImage = fromUser.image
        )
    }

    private val msgTxtChangeListener=object : TextWatcher{
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
        val message=messageList.get(position)
        if (message.type=="image" && message.imageMessage!!.imageType=="image") {
            binding.fullSizeImageView.show()
            StfalconImageViewer.Builder(
                context,
                listOf(MyImage(messageList.get(position).imageMessage?.uri!!))
            ) { imageView, myImage ->
                ImageUtils.loadGalleryImage(myImage.url, imageView)
            }
                .withDismissListener { binding.fullSizeImageView.visibility = View.GONE }
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_ADD_CONTACT){
            if (resultCode == Activity.RESULT_OK) {
                binding.viewChatHeader.imageAddContact.gone()
                val contacts=UserUtils.fetchContacts(requireContext())
                val savedName=contacts.firstOrNull { it.mobile==chatUser.user.mobile?.number }
                savedName?.let {
                    binding.viewChatHeader.txtLocalName.text=it.name
                    chatUser.localName=it.name
                    chatUser.locallySaved=true
                    viewModel.insertUser(chatUser)
                }
            }else if (resultCode == Activity.RESULT_CANCELED) {
                Timber.v("Cancelled Added Contact")
            }
        }else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
            onCropResult(data)
        else
            ImageUtils.cropImage(requireActivity(), data, true)
    }

    private fun onCropResult(data: Intent?) {
        try {
            val imagePath: Uri? = ImageUtils.getCroppedImage(data)
            if (imagePath!=null){
                val message=createMessage().apply {
                    type="image"
                    imageMessage=ImageMessage(imagePath.toString())
                    chatUsers= ArrayList()
                }
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
        }else if (Utils.isPermissionOk(*grantResults))
            openSaveIntent()
    }

    override fun onCommitContent(inputContentInfo: InputContentInfoCompat?,
        flags: Int,
        opts: Bundle?) {
        val imageMsg=createMessage()
        val image=ImageMessage("${inputContentInfo?.contentUri}")
        image.imageType=if(image.uri.toString().endsWith(".png")) "sticker" else "gif"
        imageMsg.apply {
            type="image"
            imageMessage=image
            chatUsers= ArrayList()
        }
        viewModel.uploadToCloud(imageMsg,image.toString())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAttachmentItemClicked(event: BottomSheetEvent){
        when(event.position){
            0->{
                ImageUtils.takePhoto(requireActivity())
            }
            1->{
                ImageUtils.chooseGallery(requireActivity())
            }
            2->{
                //create intent for gallery video
            }
            3->{
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


    override fun onResume() {
        viewModel.setSeenAllMessage()
        preference.setCurrentUser(chatUserId)
        viewModel.sendCachedTxtMesssages()
        Utils.removeNotification(requireContext())
        super.onResume()
    }

    override fun onDestroy() {
        Utils.closeKeyBoard(requireActivity())
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        preference.clearCurrentUser()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecording()
        AdChat.stopPlaying()
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
                AdChat.messageList = messageList
                adChat.submitList(messageList)
                msgPostponed=false
            }
        }
    }

    companion object{
        private const val REQ_ADD_CONTACT=22
    }
}

