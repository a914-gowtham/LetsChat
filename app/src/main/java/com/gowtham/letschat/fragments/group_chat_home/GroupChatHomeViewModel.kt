package com.gowtham.letschat.fragments.group_chat_home

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.utils.MPreference
import dagger.hilt.android.qualifiers.ApplicationContext

class GroupChatHomeViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context,
    private val preference: MPreference,
    private val docuRef: DocumentReference,
    private val dbRepository: DbRepository,
    private val usersCollection: CollectionReference) : ViewModel()  {


    fun getGroupMessages() = dbRepository.getGroupWithMessages()



}