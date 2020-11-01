package com.gowtham.letschat.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.CollectionReference
import com.gowtham.letschat.R
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.MPreference
import com.gowtham.letschat.utils.UserUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActSplash : AppCompatActivity() {

    @Inject
    lateinit var preference: MPreference

    @Inject
    lateinit var userCollection: CollectionReference

    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_splash)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        UserUtils.updatePushToken(this, userCollection,false)
        sharedViewModel.onFromSplash()
        sharedViewModel.openMainAct.observe(this, {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        })
    }
}