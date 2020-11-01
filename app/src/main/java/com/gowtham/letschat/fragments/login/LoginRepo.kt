package com.gowtham.letschat.fragments.login

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.gowtham.letschat.models.Country
import com.gowtham.letschat.ui.activities.MainActivity
import com.gowtham.letschat.utils.LogInFailedState
import com.gowtham.letschat.utils.printMeD
import com.gowtham.letschat.utils.toast
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LoginRepo @Inject constructor(@ActivityRetainedScoped val actContxt: MainActivity,
                                    @ApplicationContext val context: Context) :
    PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

    private val verificationId: MutableLiveData<String> = MutableLiveData()

    private val credential: MutableLiveData<PhoneAuthCredential> = MutableLiveData()

    private val taskResult: MutableLiveData<Task<AuthResult>> = MutableLiveData()

    private val failedState: MutableLiveData<LogInFailedState> = MutableLiveData()

    init {
        "LoginRepo init".printMeD()
    }

    fun setMobile(country: Country, mobile: String) {
        val number = country.noCode + " " + mobile
        "LoginRepo:: $number".printMeD()
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            number,
            60,
            TimeUnit.SECONDS,
            actContxt,
            this
        )
    }

    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        Log.d("TAG", "onVerificationCompleted:$credential")
        this.credential.value = credential
        Handler().postDelayed({
            signInWithPhoneAuthCredential(credential)
        }, 1000)
    }

    override fun onVerificationFailed(exp: FirebaseException) {
        failedState.value = LogInFailedState.Verification
        "onVerficationFailed:: ${exp.message}".printMeD()
        when (exp) {
            is FirebaseAuthInvalidCredentialsException ->
                context.toast("Invalid Request")
            else -> context.toast(exp.message.toString())
        }
    }

    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
        Log.d("TAG", "onCodeSent:$verificationId")
        this.verificationId.value = verificationId
        context.toast("Verification code sent successfully")
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("TAG", "signInWithCredential:success")
                    taskResult.value = task
                } else {
                    Log.w("TAG", "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException)
                        context.toast("Invalid verification code!")
                    failedState.value = LogInFailedState.SignIn
                }
            }
    }

    fun setCredential(credential: PhoneAuthCredential) {
        signInWithPhoneAuthCredential(credential)
    }

    fun getVCode(): MutableLiveData<String> {
        return verificationId
    }

    fun setVCodeNull() {
        verificationId.value = null
    }

    fun clearOldAuth(){
        credential.value=null
        taskResult.value=null
    }

    fun getCredential(): LiveData<PhoneAuthCredential> {
        return credential
    }

    fun getTaskResult(): LiveData<Task<AuthResult>> {
        return taskResult
    }

    fun getFailed(): LiveData<LogInFailedState> {
        return failedState
    }
}