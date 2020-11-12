package com.gowtham.letschat.fragments.single_chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.gowtham.letschat.databinding.FSingleChatBinding
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.ImageMessage
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.db.data.TextMessage
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.*
import com.gowtham.letschat.views.CustomEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject


@AndroidEntryPoint
class FSingleChat : Fragment(), ItemClickListener,CustomEditText.KeyBoardInputCallbackListener {

    private lateinit var binding: FSingleChatBinding

    @Inject
    lateinit var preference: MPreference

    @Inject
    lateinit var chatUserDao: ChatUserDao

    private lateinit var chatUser: ChatUser

    private lateinit var fromUser: UserProfile

    private lateinit var toUser: UserProfile

    private var messageList = mutableListOf<Message>()

    private val viewModel: SingleChatViewModel by viewModels()

    private lateinit var localUserId: String

    val args by navArgs<FSingleChatArgs>()

    private lateinit var manager: LinearLayoutManager

    private lateinit var chatUserId: String

    private val REQ_ADD_CONTACT=22

    private val adChat: AdChat by lazy {
        AdChat(requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = FSingleChatBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewmodel=viewModel
        chatUser= args.chatUserProfile!!
        viewModel.setUnReadCountZero(chatUser)
        binding.viewChatBtm.lottieSend.setOnClickListener {
            sendMessage()
        }
        binding.viewChatHeader.viewBack.setOnClickListener {
           findNavController().popBackStack()
        }
        binding.viewChatHeader.imageAddContact.setOnClickListener {
            if (Utils.askContactPermission(this))
                openSaveIntent()
        }
        if(!chatUser.locallySaved)
            binding.viewChatHeader.imageAddContact.show()
        viewModel.canScroll(false)
        binding.viewChatBtm.edtMsg.setKeyBoardInputCallbackListener(this)
        setDataInView()
        subscribeObservers()

        lifecycleScope.launch {
            viewModel.getMessagesByChatUserId(chatUserId).collect { mMessagesList ->
                messageList = mMessagesList as MutableList<Message>
                AdChat.messageList = messageList
                adChat.submitList(mMessagesList)
                //scroll to last items in recycler (recent messages)
                if (messageList.isNotEmpty()) {
                    viewModel.setChatsOfThisUser(messageList)
                    if (viewModel.getCanScroll())  //scroll only if new message arrived
                        binding.listMessage.smoothScrollToPos(messageList.lastIndex)
                    else
                        viewModel.canScroll(true)
                    LogMessage.v("Last Message ${messageList.last().createdAt}")
                }
            }
        }
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
        val msg = binding.viewChatBtm.edtMsg.text!!.trim().toString()
        if (msg.isEmpty())
            return
        binding.viewChatBtm.lottieSend.playAnimation()
        val message = createMessage()
        message.textMessage=TextMessage(msg)
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

    private fun subscribeObservers() {
        //pass messages list for recycler to show
          viewModel.chatUserOnlineStatus.observe(viewLifecycleOwner, {
              Utils.setOnlineStatus(binding.viewChatHeader.txtLastSeen, it, localUserId)
          })
    }

    override fun onResume() {
        viewModel.setOnline(true)
        preference.setCurrentUser(chatUserId)
        viewModel.setSeenAllMessage()
        viewModel.sendCachedMesssages()
        Utils.removeNotification(requireContext())
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        preference.clearCurrentUser()
        viewModel.setOnline(false)
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val msgTxtChangeListener=object : TextWatcher{
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
               viewModel.sendTyping(binding.viewChatBtm.edtMsg.trim())
        }

        override fun afterTextChanged(s: Editable?) {
             if (s.isNullOrEmpty())
                 binding.viewChatBtm.imageStreography.show()
             else
                 binding.viewChatBtm.imageStreography.gone()
        }
    }

    override fun onItemClicked(v: View, position: Int) {


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
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Utils.isPermissionOk(*grantResults))
            openSaveIntent()
    }

    override fun onDestroy() {
        Utils.closeKeyBoard(requireActivity())
        super.onDestroy()
    }

    override fun onCommitContent(inputContentInfo: InputContentInfoCompat?,
        flags: Int,
        opts: Bundle?) {
        val f=Uri.fromFile(File(inputContentInfo?.contentUri.toString()))
        val uri=inputContentInfo?.contentUri.toString()
        val uu=FileUtils.getPath(requireContext(),inputContentInfo?.contentUri!!)

        val imageMsg=createMessage()
        val image=ImageMessage("${inputContentInfo.contentUri}")
        image.imageType=if(uu.endsWith(".png")) "sticker" else "gif"
        imageMsg.apply {
            type="image"
            imageMessage=image
        }
        viewModel.sendStickerOrGif(imageMsg)
    }
}

