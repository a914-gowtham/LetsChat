package com.gowtham.letschat.core

import android.content.Context
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.gowtham.letschat.FirebasePush
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.db.daos.GroupMessageDao
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.di.GroupCollection
import com.gowtham.letschat.fragments.single_chat.toDataClass
import com.gowtham.letschat.utils.LogMessage
import com.gowtham.letschat.utils.MPreference
import com.gowtham.letschat.utils.UserUtils
import com.gowtham.letschat.utils.Utils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupChatHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preference: MPreference,
    private val userCollection: CollectionReference,
    @GroupCollection
    private val groupCollection: CollectionReference,
    private val dbRepository: DbRepository) {

    private val userId = preference.getUid()

    private lateinit var messageCollectionGroup: Query

    private val messagesList: MutableList<GroupMessage> by lazy { mutableListOf() }

    private val listOfGroup = ArrayList<String>()

    private lateinit var chatUsers: List<ChatUser>

    private var instanceCreated=false

    companion object{

        private var groupListener: ListenerRegistration?=null

        private var myProfileListener: ListenerRegistration?=null

        fun removeListener(){
            groupListener?.remove()
            myProfileListener?.remove()
        }
    }

    fun initHandler() {
        if (instanceCreated)
            return
        else
            instanceCreated=true
        Timber.v("GroupChatHandler init")
        preference.clearCurrentGroup()
        messageCollectionGroup = UserUtils.getGroupMsgSubCollectionRef()
        addGroupsSnapShotListener()
        addGroupMsgListener()
    }

    private fun addGroupMsgListener() {
       groupListener= messageCollectionGroup.whereArrayContains("to", userId!!)
            .addSnapshotListener { snapshots, error ->
                if (error == null) {
                    messagesList.clear()
                    listOfGroup.clear()
                    if(snapshots==null)
                        return@addSnapshotListener
                    for (msgDoc in snapshots) {
                        if (msgDoc.id.toLong() < preference.getLogInTime())
                            continue    //ignore old messages
                        val message = msgDoc.data.toDataClass<GroupMessage>()
                        if (message.groupId == preference.getOnlineGroup()) { //would be updated by snapshot listener
                            continue
                        }
                        if (!listOfGroup.contains(message.groupId))
                            listOfGroup.add(message.groupId)
                        messagesList.add(message)
                    }
                    updateGroupUnReadCount()
                }else
                    Timber.v(error)
            }
    }

    private fun updateGroupUnReadCount() {
        CoroutineScope(Dispatchers.IO).launch {
            val groups=dbRepository.getGroupList()
            for (group in groups){
                group.unRead=messagesList.filter {
                    val myStatus= Utils.myMsgStatus(userId.toString(),it)
                    it.from!=userId &&
                    it.groupId==group.id && myStatus<3
                }.size
            }
            updateInLocal(groups)
        }

    }

    private fun updateInLocal(groups: List<Group>) {
        val updateToSeen = GroupMsgStatusUpdater(groupCollection)
        updateToSeen.updateToDelivery(userId!!, messagesList, *listOfGroup.toTypedArray())
            dbRepository.insertMultipleGroupMessage(messagesList)
            dbRepository.insertMultipleGroup(groups)
        if (groups.isNotEmpty())
            FirebasePush.showGroupNotification(context, dbRepository)
    }

    private fun addGroupsSnapShotListener() {
        myProfileListener= userCollection.document(userId.toString()).addSnapshotListener { snapshot, error ->
            if (error == null) {
                val groups = snapshot?.get("groups")
                val listOfGroup = if (groups == null) ArrayList() else groups as ArrayList<String>
                CoroutineScope(Dispatchers.IO).launch {
                    val alreadySavedGroup = dbRepository.getGroupList().map { it.id }
                    val removedGroups = alreadySavedGroup.toSet().minus(listOfGroup.toSet())
                    val newGroups = listOfGroup.toSet().minus(alreadySavedGroup.toSet())
                    withContext(Dispatchers.Main) {
                        queryNewGroups(newGroups)
                    }
                }
            }
        }
    }

    private fun queryNewGroups(newGroups: Set<String>) {
        Timber.v("New groups ${newGroups.size}")
        for (groupId in newGroups) {
            val groupQuery = GroupQuery(groupId, dbRepository,preference)
            groupQuery.getGroupData(groupCollection)
        }
    }

}