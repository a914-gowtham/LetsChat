package com.gowtham.letschat.fragments.add_group_members

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gowtham.letschat.core.QueryCompleteListener
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.LoadState
import com.gowtham.letschat.utils.LogMessage
import com.gowtham.letschat.utils.UserUtils
import com.gowtham.letschat.utils.toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class AddGroupViewModel @ViewModelInject
              constructor(@ApplicationContext context: Context,
                          private val usersDao: ChatUserDao) : ViewModel() {

    private val chipList= MutableLiveData<ArrayList<ChatUser>>()

    private val allContacts= ArrayList<ChatUser>()

    val queryState = MutableLiveData<LoadState>()

    private lateinit var chatUsers: List<ChatUser>

    var isFirstCall=true

    init {
        Timber.v("AddGroupViewModel init")
        CoroutineScope(Dispatchers.IO).launch{
            chatUsers=usersDao.getChatUserList().filter { it.locallySaved }
            if (chatUsers.isNullOrEmpty())
                startQuery()
        }
    }

    fun getChatList() = usersDao.getAllChatUser()

    fun getChipList(): LiveData<ArrayList<ChatUser>>{
       return chipList
    }

    fun setChipList(list: List<ChatUser>){
        val newList=ArrayList<ChatUser>(list.filter { it.isSelected })
        chipList.value=newList
    }

    fun setContactList(list: List<ChatUser>) {
        allContacts.clear()
        allContacts.addAll(list)
    }

    fun getContactList() = allContacts

    private fun startQuery() {
        try {
            queryState.postValue(LoadState.OnLoading)
            val success= UserUtils.updateContactsProfiles(onQueryCompleted)
            if (!success) {
                Timber.v("Recursion error")
                queryState.postValue(LoadState.OnFailure(java.lang.Exception("Recursion exception")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        AdChip.allAddedContacts.clear()
        allContacts.clear()
        isFirstCall=true
        Timber.v("OnClear AddGroup")
        super.onCleared()
    }

    private val onQueryCompleted=object : QueryCompleteListener {
        override fun onQueryCompleted(queriedList: ArrayList<UserProfile>) {
            try {
                val localContacts=UserUtils.fetchContacts(context)
                val finalList = ArrayList<ChatUser>()
                val queriedList=UserUtils.queriedList

                //set localsaved name to queried users
                for(doc in queriedList){
                    val savedNumber=localContacts.firstOrNull { it.mobile == doc.mobile?.number }
                    if(savedNumber!=null){
                        val chatUser= UserUtils.getChatUser(doc, chatUsers, savedNumber.name)
                        finalList.add(chatUser)
                    }
                }
                queryState.value=LoadState.OnSuccess(finalList)
                CoroutineScope(Dispatchers.IO).launch {
                    usersDao.insertMultipleUser(finalList)
                }
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



}