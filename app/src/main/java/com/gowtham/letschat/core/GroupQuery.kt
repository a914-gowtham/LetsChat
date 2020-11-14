package com.gowtham.letschat.core

import com.google.firebase.firestore.CollectionReference
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.utils.MPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class GroupQuery(private val groupId: String,private val dbRepository: DbRepository,private val preference: MPreference) {

    private val myUserId=preference.getUid()!!

   fun getGroupData(groupCollection: CollectionReference){
       val userId=preference.getUid()
       groupCollection.document(groupId).get().addOnSuccessListener { snapshot->
           snapshot?.let { data ->
               if (!data.exists())
                   return@addOnSuccessListener
               val group=data.toObject(Group::class.java)
               val profiles=group?.profiles
               val index=profiles!!.indexOfFirst { it.uId==userId }
               profiles.removeAt(index)
               profiles.add(0,preference.getUserProfile()!!)  //moving localuser to 0 th index
               group.profiles=profiles
               CoroutineScope(Dispatchers.IO).launch {
                   checkAlreadySavedMember(group, dbRepository.getChatUserList())
               }
           }
       }.addOnFailureListener {
           Timber.v("GroupDataGrtting failed ${it.message}")
       }
    }

    private fun checkAlreadySavedMember(group: Group, list: List<ChatUser>){
         val chatUsers= ArrayList<ChatUser>()
        for (profile in group.profiles!!){
            if (profile.uId==myUserId) {
                chatUsers.add(ChatUser(myUserId, "You", profile))
                continue
            }
            val chatUser=list.firstOrNull { it.id==profile.uId }
            if (chatUser==null){
                val localName="${profile.mobile?.country} ${profile.mobile?.number}"
                val user=ChatUser(profile.uId.toString(),localName,profile)
                chatUsers.add(user)
            }else
                chatUsers.add(chatUser)
        }
        group.members=chatUsers
        group.profiles= ArrayList()
        dbRepository.insertMultipleUser(chatUsers)
        dbRepository.insertGroup(group)
    }

}