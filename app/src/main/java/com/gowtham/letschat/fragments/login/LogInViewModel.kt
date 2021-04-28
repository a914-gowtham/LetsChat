package com.gowtham.letschat.fragments.login

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.FirebaseFirestore
import com.gowtham.letschat.R
import com.gowtham.letschat.TYPE_LOGGED_IN
import com.gowtham.letschat.models.Country
import com.gowtham.letschat.models.ModelMobile
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.android.scopes.ActivityScoped
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@HiltViewModel
class LogInViewModel @Inject
constructor(@ApplicationContext private val context: Context,
            private val logInRepo: LoginRepo, private val preference: MPreference) :
    ViewModel() {

    val country = MutableLiveData<Country>()

    val mobile = MutableLiveData<String>()

    val userProfileGot=MutableLiveData<Boolean>()

    private val progress = MutableLiveData(false)

    private val verifyProgress = MutableLiveData(false)

    var canResend: Boolean = false

    val resendTxt = MutableLiveData<String>()

    val otpOne = MutableLiveData<String>()

    val otpTwo = MutableLiveData<String>()

    val otpThree = MutableLiveData<String>()

    val otpFour = MutableLiveData<String>()

    val otpFive = MutableLiveData<String>()

    val otpSix = MutableLiveData<String>()

    var ediPosition = 0

    var verifyCode: String = ""

    private lateinit var timer: CountDownTimer

    init {
        "LogInViewModel init".printMeD()
    }

    fun setCountry(country: Country) {
        this.country.value = country
    }

    fun setMobile() {
        logInRepo.clearOldAuth()
        saveMobile()
        logInRepo.setMobile(country.value!!, mobile.value!!)
    }

    fun setProgress(show: Boolean) {
        progress.value = show
    }

    fun getProgress(): LiveData<Boolean> {
        return progress
    }

    fun resendClicked() {
        "Resend Clicked".printMeD()
        if (canResend) {
            setVProgress(true)
            setMobile()
        }
    }

    fun startTimer() {
        try {
            canResend = false
            timer = object : CountDownTimer(60000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    setTimerTxt(millisUntilFinished / 1000)
                }

                override fun onFinish() {
                    canResend = true
                    resendTxt.value = "Resend"
                }
            }
            timer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetTimer() {
        canResend = false
        resendTxt.value = ""
        if (this::timer.isInitialized)
        timer.cancel()
    }

    private fun setTimerTxt(seconds: Long) {
        try {
            val s = seconds % 60
            val m = seconds / 60 % 60
            if (s == 0L && m == 0L) return
            val resend: String =
                context.getString(R.string.txt_resend) + " in " + String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    m,
                    s
                )
            resendTxt.value = resend
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun setEmptyText(){
        otpOne.value=""
        otpTwo.value=""
        otpThree.value=""
        otpFour.value=""
        otpFive.value=""
        otpSix.value=""
    }

    fun setVProgress(show: Boolean) {
        verifyProgress.value = show
    }

    fun getVProgress(): LiveData<Boolean> {
        return verifyProgress
    }

    fun getCredential(): LiveData<PhoneAuthCredential> {
        return logInRepo.getCredential()
    }

    fun setCredential(credential: PhoneAuthCredential) {
        setVProgress(true)
        logInRepo.setCredential(credential)
    }

    fun setVCodeNull(){
        verifyCode=logInRepo.getVCode().value!!
        logInRepo.setVCodeNull()
    }

    fun getVerificationId(): MutableLiveData<String> {
        return logInRepo.getVCode()
    }

    fun getTaskResult(): LiveData<Task<AuthResult>> {
        return logInRepo.getTaskResult()
    }

    fun getFailed(): LiveData<LogInFailedState> {
        return logInRepo.getFailed()
    }

    private fun saveMobile() =
       preference.saveMobile(ModelMobile(country.value!!.noCode,mobile.value!!))

    fun fetchUser(taskId: Task<AuthResult>) {
        val db = FirebaseFirestore.getInstance()
        val user = taskId.result?.user
        Timber.v("FetchUser:: ${user?.uid}")
        val noteRef = db.document("Users/" + user?.uid)
        noteRef.get()
            .addOnSuccessListener { data ->
                Timber.v("Uss:: ${preference.getUid()}")
                preference.setUid(user?.uid.toString())
                Timber.v("Uss11:: ${preference.getUid()}")
                preference.setLogin()
                preference.setLogInTime()
                setVProgress(false)
                progress.value=false
                if (data.exists()) {
                    val appUser = data.toObject(UserProfile::class.java)
                    Timber.v("UserId ${appUser?.uId}")
                    preference.saveProfile(appUser!!)
                    //if device id is not same,send new_user_logged type notification to the token
                   checkLastDevice(appUser)
                }
                userProfileGot.value=true
            }.addOnFailureListener { e ->
                setVProgress(false)
                progress.value=false
                context.toast(e.message.toString())
            }
    }

    private fun checkLastDevice(appUser: UserProfile?) {
        try {
            if (appUser!=null){
                val localDevice = UserUtils.getDeviceId(context)
                val deviceDetails=appUser.deviceDetails
                val sameDevice=deviceDetails?.device_id.equals(localDevice)
                if (!sameDevice)
                    UserUtils.sendPush(context,TYPE_LOGGED_IN,"", appUser.token,appUser.uId!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAll(){
        userProfileGot.value=false
        logInRepo.clearOldAuth()
    }
}