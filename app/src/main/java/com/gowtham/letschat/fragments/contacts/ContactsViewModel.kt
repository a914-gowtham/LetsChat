package com.gowtham.letschat.fragments.contacts

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.gowtham.letschat.core.QueryCompleteListener
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class ContactsViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context,
    private val usersDao: ChatUserDao,
    private val preference: MPreference) : ViewModel() {

    val queryState = MutableLiveData<LoadState>()

    val list= MutableLiveData<ArrayList<ChatUser>>()

    val contactsCount = MutableLiveData("0 Contacts")

    private val uId=preference.getUid()

    private lateinit var chatUsers: List<ChatUser>

    init {
        LogMessage.v("ContactsViewModel init")
        CoroutineScope(Dispatchers.IO).launch{
            chatUsers=usersDao.getChatUserList()
        }
    }

    fun getContacts()=usersDao.getAllChatUser()

    fun setContactCount(size: Int) {
        contactsCount.value="$size Contacts"
    }

    fun startQuery() {
        try {
            queryState.value=LoadState.OnLoading
            val success=UserUtils.updateContactsProfiles(onQueryCompleted)
            if (!success)
                queryState.value=LoadState.OnFailure(java.lang.Exception("Recursion exception"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val onQueryCompleted=object : QueryCompleteListener {
        override fun onQueryCompleted(queriedList: ArrayList<UserProfile>) {
            try {
                LogMessage.v("Query Completed ${UserUtils.queriedList.size}")
                val localContacts=UserUtils.fetchContacts(context)
                val finalList = ArrayList<ChatUser>()
                val queriedList=UserUtils.queriedList

                //set localsaved name to queried users
                for(doc in queriedList){
                    val savedNumber=localContacts.firstOrNull { it.mobile == doc.mobile?.number }
                    if(savedNumber!=null){
                        val chatUser= UserUtils.getChatUser(doc, chatUsers, savedNumber.name)
                        Timber.v("Contact ${chatUser.documentId}")
                        finalList.add(chatUser)
                    }
                }
                contactsCount.value="${finalList.size} Contacts"
                queryState.value=LoadState.OnSuccess(finalList)
                CoroutineScope(Dispatchers.IO).launch {
                    usersDao.insertMultipleUser(finalList)
                }
                context.toast("Contacts refreshed")
                setDefaultValues()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setDefaultValues() {
        //set default values
        UserUtils.totalRecursionCount=0
        UserUtils.resultCount=0
        UserUtils.queriedList.clear()
    }

    override fun onCleared() {
        LogMessage.v("ContactsViewModel OnCleared")
        super.onCleared()
    }

}