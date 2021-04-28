package com.gowtham.letschat.fragments.create_group

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.gowtham.letschat.TYPE_NEW_GROUP
import com.gowtham.letschat.db.DbRepository
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.di.GroupCollection
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preference: MPreference,
    private val userCollection: CollectionReference,
    private val dbRepo: DbRepository,
    @GroupCollection
    private val groupCollection: CollectionReference) : ViewModel() {

    val progressProPic = MutableLiveData(false)

    val groupName = MutableLiveData("")

    val imageUrl = MutableLiveData("")

    val groupCreateStatus = MutableLiveData<LoadState>()

    private val storageRef=UserUtils.getStorageRef(context)

    fun uploadProfileImage(imagePath: Uri) {
        try {
            progressProPic.value = true
            val child = storageRef.child("group_${System.currentTimeMillis()}.jpg")
            val task = child.putFile(imagePath)
            task.addOnSuccessListener {
                child.downloadUrl.addOnCompleteListener { taskResult ->
                    progressProPic.value = false
                    imageUrl.value = taskResult.result.toString()
                }.addOnFailureListener {
                    OnFailureListener { e ->
                        progressProPic.value = false
                        context.toast(e.message.toString())
                    }
                }
            }.addOnProgressListener { taskSnapshot ->
                val progress: Double =
                    100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createGroup(memberList: ArrayList<ChatUser>) {
        groupCreateStatus.value=LoadState.OnLoading
        val gName=groupName.value+"_${Random.nextInt(0,100)}"  //
        memberList.add(0,ChatUser(preference.getUid()!!,"You",preference.getUserProfile()!!))
        val listOfProfiles=memberList.map { it.user } as ArrayList<UserProfile>
        val groupData=Group(gName, preference.getUid()!!,
            System.currentTimeMillis(),"",imageUrl.value.toString(),null,listOfProfiles)

        groupCollection.document(gName).set(groupData, SetOptions.merge()).
        addOnSuccessListener {
            updateGroupInEveryUserProfile(groupData,memberList)
        }.addOnFailureListener { exception->
            groupCreateStatus.value=LoadState.OnFailure(exception)
            context.toast(exception.message.toString())
        }
    }

    private fun updateGroupInEveryUserProfile(group: Group, memberList: ArrayList<ChatUser>) {
        group.members = memberList
        group.profiles = ArrayList()
        val listOfIds = memberList.map { it.id }
        val batch = FirebaseFirestore.getInstance().batch()
        for (id in listOfIds) {
            val userDoc = userCollection.document(id)
            batch.set(
                userDoc, mapOf("groups" to FieldValue.arrayUnion(group.id)),
                SetOptions.merge()
            )
        }
        batch.commit().addOnSuccessListener {
            LogMessage.v("Batch update success for group Creation")
            groupCreateStatus.value = LoadState.OnSuccess(group)
            dbRepo.insertGroup(group)
            val groupdata = Group(group.id)
            for (user in group.members!!) {
                val token = user.user.token
                if (token.isNotEmpty())
                    UserUtils.sendPush(context, TYPE_NEW_GROUP,
                        Json.encodeToString(groupdata), token, user.id)
            }
        }.addOnFailureListener { exception ->
            LogMessage.v("Batch update failure ${exception.message}  for group Creation")
            groupCreateStatus.value = LoadState.OnFailure(exception)
            context.toast(exception.message.toString())
        }
    }

}