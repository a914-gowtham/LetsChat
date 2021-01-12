package com.gowtham.letschat

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.LifecycleObserver
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.LogMessage
import com.gowtham.letschat.utils.MPreference
import com.gowtham.letschat.utils.UserUtils
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MApplication : MultiDexApplication(), LifecycleObserver,Configuration.Provider {

    @Inject
    lateinit var preference: MPreference

    @Inject
    lateinit var userDao: ChatUserDao

    @Inject
    lateinit var messageDao: MessageDao

    @Inject
    lateinit var userCollection: CollectionReference

    @Inject lateinit var workerFactory: HiltWorkerFactory

    companion object {
        lateinit var instance: MApplication
            private set
        var isAppRunning = false
        lateinit var appContext: Context
        lateinit var userDaoo: ChatUserDao
        lateinit var messageDaoo: MessageDao
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = this
        userDaoo = userDao
        messageDaoo=messageDao
        FirebaseApp.initializeApp(this)
        initTimber()
        if (preference.isLoggedIn())
            checkLastDevice()   //looking for does user is logged in another device.if yes,need to shoe dialog for log in again
    }


    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return "LetsChat/${element.fileName}:${element.lineNumber})#${element.methodName}"
                }
            })
        }
    }

    private fun checkLastDevice() {
        userCollection.document(preference.getUid()!!).get().addOnSuccessListener { data ->
            Timber.v("Device Checked")
            val appUser = data.toObject(UserProfile::class.java)
            checkDeviceDetails(appUser)
        }.addOnFailureListener { e ->
            LogMessage.v(e.message.toString())
        }
    }

    private fun checkDeviceDetails(appUser: UserProfile?) {
        val device = appUser?.deviceDetails
        val localDevice = UserUtils.getDeviceId(this)
        if (device != null) {
            val sameDevice = device.device_id.equals(localDevice)
            preference.setLastDevice(sameDevice)
            Timber.v("Device Checked ${device.device_id.equals(localDevice)}")
            if (sameDevice)
                UserUtils.updatePushToken(this,userCollection, true)
        }
    }
}