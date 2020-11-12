package com.gowtham.letschat.core

import android.content.Context
import com.google.firebase.firestore.CollectionReference
import com.gowtham.letschat.FirebasePush
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.OnSuccessListener
import com.gowtham.letschat.utils.UserUtils
import com.gowtham.letschat.utils.Utils

class ChatUserUtil(private val dbRepository: DbRepository,
                   private val usersCollection: CollectionReference,
                   private val listener: OnSuccessListener?) {

    fun queryNewUserProfile(context: Context,chatUserId: String,docId: String?, unReadCount: Int=1) {
        try {
            usersCollection.document(chatUserId)
                .get().addOnSuccessListener { profile ->
                    if (profile.exists()) {
                        val userProfile = profile.toObject(UserProfile::class.java)
                        val mobile = userProfile?.mobile?.country + " " + userProfile?.mobile?.number
                        val chatUser = ChatUser(userProfile?.uId!!, mobile, userProfile)
                        chatUser.unRead=unReadCount
                        docId?.let {
                            chatUser.documentId=it
                        }
                        if (Utils.isContactPermissionOk(context)) {
                            val contacts = UserUtils.fetchContacts(context)
                            val savedContact=contacts.firstOrNull { it.mobile.contains(userProfile.mobile!!.number) }
                            savedContact?.let {
                                chatUser.localName=it.name
                                chatUser.locallySaved=true
                            }
                        }
                        dbRepository.insertUser(chatUser)
                        listener?.onResult(true,chatUser)
//                      Utils.removeNotification(context)
                        FirebasePush.showNotification(context,dbRepository)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}