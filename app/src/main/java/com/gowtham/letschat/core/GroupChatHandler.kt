package com.gowtham.letschat.core

import android.content.Context
import com.google.firebase.firestore.*
import com.gowtham.letschat.FirebasePush
import com.gowtham.letschat.db.DbRepository
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
    private val dbRepository: DbRepository,
    private val groupMsgStatusUpdater: GroupMsgStatusUpdater
) {

    private var userId = preference.getUid()

    private lateinit var messageCollectionGroup: Query

    private val messagesList = mutableListOf<GroupMessage>()

    private val listOfGroup = ArrayList<String>()

    private var isFirstQuery = false

    companion object {
        private var groupListener: ListenerRegistration? = null
        private var myProfileListener: ListenerRegistration? = null
        private var instanceCreated = false

        fun removeListener() {
            instanceCreated = false
            groupListener?.remove()
            myProfileListener?.remove()
        }
    }

    fun initHandler() {
        if (instanceCreated)
            return
        else
            instanceCreated = true
        userId = preference.getUid()
        Timber.v("GroupChatHandler init")
        preference.clearCurrentGroup()
        messageCollectionGroup = UserUtils.getGroupMsgSubCollectionRef()
        addGroupsSnapShotListener()
        addGroupMsgListener()
    }

    private fun addGroupMsgListener() {
        try {
            groupListener = messageCollectionGroup.whereArrayContains("to", userId!!)
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null || snapshots.metadata.isFromCache) {
                        LogMessage.v("Error ${error?.localizedMessage}")
                        return@addSnapshotListener
                    }
                    messagesList.clear()
                    listOfGroup.clear()

                    onSnapShotChanged(snapshots)

                    if (messagesList.isNotEmpty())
                        updateGroupUnReadCount()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun onSnapShotChanged(snapshots: QuerySnapshot) {
        if(isFirstQuery){
            snapshots.forEach { doc->
                val message = doc.data.toDataClass<GroupMessage>()
                if (!listOfGroup.contains(message.groupId))
                    listOfGroup.add(message.groupId)
                messagesList.add(message)
            }
            isFirstQuery=false
        }
        else
        for (shot in snapshots.documentChanges) {
            if (shot.type == DocumentChange.Type.ADDED ||
                shot.type == DocumentChange.Type.MODIFIED
            ) {
                val message = shot.document.data.toDataClass<GroupMessage>()
                if (!listOfGroup.contains(message.groupId))
                    listOfGroup.add(message.groupId)
                messagesList.add(message)
            }
        }
    }

    private fun updateGroupUnReadCount() {
        CoroutineScope(Dispatchers.IO).launch {
            dbRepository.insertMultipleGroupMessage(messagesList)
            val groupsWithMsgs = dbRepository.getGroupWithMessagesList()
            messagesList.clear()
            for (groupWithMsg in groupsWithMsgs) {
                val unreadCount = groupWithMsg.messages.filter {
                    val myStatus = Utils.myMsgStatus(userId.toString(), it)
                    it.from != userId &&
                            it.groupId == groupWithMsg.group.id && myStatus < 3
                }.size
                groupWithMsg.group.unRead =
                    if (preference.getOnlineGroup() == groupWithMsg.group.id) 0
                    else unreadCount
                messagesList.addAll(groupWithMsg.messages)
            }
            val groups = groupsWithMsgs.map {
                it.group
            }
            dbRepository.insertMultipleGroup(groups)
            changeMsgStatus(groups)
        }
    }

    private fun changeMsgStatus(groups: List<Group>) {
        if (groups.isNotEmpty())
            FirebasePush.showGroupNotification(context, dbRepository)
        val currentOnlineGroupId=preference.getOnlineGroup()
        if(currentOnlineGroupId.isNotEmpty()){
            val currentGroupMsgs = messagesList.filter {
                it.groupId == currentOnlineGroupId
            }
            val otherGroupMsgs = messagesList.filter {
                it.groupId != currentOnlineGroupId
            }
            groupMsgStatusUpdater.updateToSeen(userId!!, currentGroupMsgs,currentOnlineGroupId)
            groupMsgStatusUpdater.updateToDelivery(userId!!, otherGroupMsgs, *listOfGroup.toTypedArray())
        }else
            groupMsgStatusUpdater.updateToDelivery(userId!!, messagesList, *listOfGroup.toTypedArray())

    }

    private fun addGroupsSnapShotListener() {
        myProfileListener =
            userCollection.document(userId.toString()).addSnapshotListener { snapshot, error ->
                if (error == null) {
                    val groups = snapshot?.get("groups")
                    val listOfGroup =
                        if (groups == null) ArrayList() else groups as ArrayList<String>
                    CoroutineScope(Dispatchers.IO).launch {
                        val alreadySavedGroup = dbRepository.getGroupList().map { it.id }
                        val removedGroups = alreadySavedGroup.toSet().minus(listOfGroup.toSet())
                        val newGroups = listOfGroup.toSet().minus(alreadySavedGroup.toSet())
                        queryNewGroups(newGroups)
                    }
                }
            }
    }

    private fun queryNewGroups(newGroups: Set<String>) {
        Timber.v("New groups ${newGroups.size}")
        for (groupId in newGroups) {
            val groupQuery = GroupQuery(groupId, dbRepository, preference)
            groupQuery.getGroupData(groupCollection)
        }
    }

}