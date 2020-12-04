package com.gowtham.letschat.fragments.myprofile

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
import com.google.firebase.storage.UploadTask
import com.gowtham.letschat.utils.LoadState
import com.gowtham.letschat.utils.MPreference
import com.gowtham.letschat.utils.toast
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.*

class FMyProfileViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context,
    private val preference: MPreference, private val storageRef: StorageReference,
    private val docuRef: DocumentReference, private val usersCollection: CollectionReference
) : ViewModel() {

    private var userProfile = preference.getUserProfile()

    val userName = MutableLiveData(userProfile?.userName)

    val imageUrl = MutableLiveData(userProfile?.image)

    val about = MutableLiveData(userProfile?.about)

    val isUploading = MutableLiveData(false)

    private val mobileData = userProfile?.mobile

    val mobile = MutableLiveData("${mobileData?.country} ${mobileData?.number}")

    val profileUpdateState = MutableLiveData<LoadState>()

    private lateinit var uploadTask: UploadTask

    init {
        Timber.v("FMyProfileViewModel init")
    }

    fun uploadProfileImage(imagePath: Uri) {
        try {
            isUploading.value = true
            val child = storageRef.child("profile_picture_${System.currentTimeMillis()}.jpg")
            if (this::uploadTask.isInitialized && uploadTask.isInProgress)
                uploadTask.cancel()
            uploadTask = child.putFile(imagePath)
            uploadTask.addOnSuccessListener {
                child.downloadUrl.addOnCompleteListener { taskResult ->
                    isUploading.value = false
                    imageUrl.value = taskResult.result.toString()
                }.addOnFailureListener {
                    OnFailureListener { e ->
                        isUploading.value = false
                        context.toast(e.message.toString())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveChanges() {
        try {
            profileUpdateState.value=LoadState.OnLoading
            updateProfileData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateProfileData() {
        try {
            val profile=userProfile!!
            profile.userName = userName.value!!.toLowerCase(Locale.getDefault())
            profile.about =about.value!!
            profile.image =imageUrl.value!!
            profile.updatedAt=System.currentTimeMillis()
            docuRef.set(profile, SetOptions.merge()).addOnSuccessListener {
                context.toast("Profile updated!")
                userProfile=profile
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
        super.onCleared()
        if (this::uploadTask.isInitialized && uploadTask.isInProgress)
            uploadTask.cancel()
    }

}
