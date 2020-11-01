package com.gowtham.letschat.fragments.profile

import android.content.Context
import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.StorageReference
import com.gowtham.letschat.models.ModelDeviceDetails
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ProfileViewModel @ViewModelInject
constructor(
    @ApplicationContext private val context: Context,
    private val preference: MPreference, private val storageRef: StorageReference,
    private val docuRef: DocumentReference,private val usersCollection: CollectionReference) : ViewModel() {

    val progressProPic = MutableLiveData(false)

    val profileUpdateState = MutableLiveData<LoadState>()

    val checkUserNameState = MutableLiveData<LoadState>()

    val name = MutableLiveData("")

    val profilePicUrl = MutableLiveData("")

    private var about = ""

    private var createdAt: Long = System.currentTimeMillis()

    init {
        LogMessage.v("ProfileViewModel")
        val userProfile = preference.getUserProfile()
        userProfile?.let {
            name.value = userProfile.userName
            profilePicUrl.value = userProfile.image
            about = userProfile.about
            createdAt = userProfile.createdAt ?: System.currentTimeMillis()
        }
    }

    fun uploadProfileImage(imagePath: Uri) {
        try {
            progressProPic.value = true
            val child = storageRef.child("profile_picture_${System.currentTimeMillis()}.jpg")
            val task = child.putFile(imagePath)
            task.addOnSuccessListener {
                child.downloadUrl.addOnCompleteListener { taskResult ->
                    progressProPic.value = false
                    profilePicUrl.value = taskResult.result.toString()
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

    private fun storeProfileData() {
        try {
            profileUpdateState.value = LoadState.OnLoading
            val profile = UserProfile(
                preference.getUid()!!, createdAt, System.currentTimeMillis(),
                profilePicUrl.value!!,name.value!!, about, mobile = preference.getMobile(),token = preference.getPushToken().toString(),
             deviceDetails =
             Json.decodeFromString<ModelDeviceDetails>(UserUtils.getDeviceInfo(context).toString()))
            docuRef.set(profile, SetOptions.merge()).addOnSuccessListener {
                preference.saveProfile(profile)
                profileUpdateState.value = LoadState.OnSuccess()
            }.addOnFailureListener { e ->
                    context.toast(e.message.toString())
                    profileUpdateState.value = LoadState.OnFailure(e)
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        LogMessage.v("ProfileViewModel Cleared")
        super.onCleared()
    }

    fun checkForUserName() {
        if (name.value.isNullOrEmpty())
            return
        checkUserNameState.value=LoadState.OnLoading
        usersCollection.whereEqualTo("userName", name.value)
            .get().addOnSuccessListener { documents ->
//                var isSameUser=false   //current user already has this name
                val userId=preference.getUid()
                val isSameuser=documents.firstOrNull() { it.id==userId }
                if (documents.isEmpty || isSameuser!=null){
                    checkUserNameState.value = LoadState.OnSuccess()
                    storeProfileData()
               } else{
                    checkUserNameState.value = LoadState.OnFailure(Exception())
                    context.toast("This user name is already taken")
                }
            }.addOnFailureListener { exception ->
                checkUserNameState.value = LoadState.OnFailure(exception)
                context.toast(exception.message.toString())
            }

    }
}