package com.gowtham.letschat.fragments.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.gowtham.letschat.models.ModelDeviceDetails
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject
constructor(
    @ApplicationContext private val context: Context,
    private val preference: MPreference
) : ViewModel() {

    val progressProPic = MutableLiveData(false)

    val profileUpdateState = MutableLiveData<LoadState>()

    val checkUserNameState = MutableLiveData<LoadState>()

    val name = MutableLiveData("")

    private val storageRef = UserUtils.getStorageRef(context)

    private val docuRef = UserUtils.getDocumentRef(context)

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

    fun storeProfileData() {
        try {
            profileUpdateState.value = LoadState.OnLoading
            val profile = UserProfile(
                preference.getUid()!!,
                createdAt,
                System.currentTimeMillis(),
                profilePicUrl.value!!,
                name.value!!.toLowerCase(Locale.getDefault()),
                about,
                mobile = preference.getMobile(),
                token = preference.getPushToken().toString(),
                deviceDetails =
                Json.decodeFromString<ModelDeviceDetails>(
                    UserUtils.getDeviceInfo(context).toString()
                )
            )
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
}