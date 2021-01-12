package com.gowtham.letschat.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.gowtham.letschat.MApplication
import com.gowtham.letschat.db.ChatUserDatabase
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.db.daos.GroupMessageDao
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.models.UserStatus
import com.gowtham.letschat.utils.*
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
open class ActBase : AppCompatActivity() {

    private val database = FirebaseDatabase.getInstance()

    @Inject
    lateinit var preference: MPreference

    @Inject
    lateinit var chatUserDao: ChatUserDao

    @Inject
    lateinit var msgDao: MessageDao

    @Inject
    lateinit var groupDao: GroupDao

    @Inject
    lateinit var messageDao: GroupMessageDao

    @Inject
    lateinit var db: ChatUserDatabase

    private var connectedRef: DatabaseReference?=null

    private val newLogInReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_LOGGED_IN_ANOTHER_DEVICE == intent.action)
                Utils.showLoggedInAlert(this@ActBase, preference,db )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        MApplication.isAppRunning=true
        registerReceiver(newLogInReceiver, IntentFilter(Constants.ACTION_LOGGED_IN_ANOTHER_DEVICE))
        if (!preference.getUid().isNullOrEmpty())
           updateStatus()
    }

    override fun onResume() {
        MApplication.isAppRunning=true
        super.onResume()
    }

    private fun updateStatus(){
        try {
            val lastOnlineRef = database.getReference("/Users/${preference.getUid()}/last_seen")
            val status = database.getReference("/Users/${preference.getUid()}/status")
            connectedRef = database.getReference(".info/connected")
            connectedRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected: Boolean = (snapshot.value ?: false) as Boolean
                    if (connected) {
                        LogMessage.v("Online status updated##")
                        status.setValue("online")
                        lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP)
                        status.onDisconnect().setValue("offline")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    LogMessage.v("Listener was cancelled at .info/connected ${error.message}")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProfileUpdated(event: UserStatus) {  //will be triggered only when initial profile update completed
      if (event.status=="online")
          updateStatus()
        else{
          val status = database.getReference("/Users/${preference.getUid()}/status")
          status.setValue("offline")
        }
    }

    override fun onDestroy() {
        MApplication.isAppRunning=false
        EventBus.getDefault().unregister(this)
        Timber.v("onDestroy")
        unregisterReceiver(newLogInReceiver)
        super.onDestroy()
    }

}