package com.gowtham.letschat.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.gowtham.letschat.R
import com.gowtham.letschat.databinding.ActivityMainBinding
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.fragments.single_chat_home.FSingleChatHomeDirections
import com.gowtham.letschat.utils.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber


class MainActivity : ActBase() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        binding.fab.setOnClickListener {
            if (Utils.askContactPermission(returnFragment()!!)) {
                if (navController.isValidDestination(R.id.FSingleChatHome))
                    navController.navigate(R.id.action_FSingleChatHome_to_FContacts)
                else if (navController.isValidDestination(R.id.FGroupChatHome))
                    navController.navigate(R.id.action_FGroupChatHome_to_FAddGroupMembers)
            }
        }
        setDataInView()
        subscribeObservers()
    }

    private fun subscribeObservers() {
        val badge = binding.bottomNav.getOrCreateBadge(R.id.nav_chat)
        badge.isVisible=false
        /*chatUserDao.getChatUserWithMessages().observe(this, { list->
            val count=list.filter { it.user.unRead!=0 }
            badge.isVisible = count.isNotEmpty() //hide if 0
            badge.number=count.size
*//*
        val badgeDrawable = binding.bottomNav.getBadge(R.id.nav_chat)
        if (badgeDrawable != null) {
            badgeDrawable.isVisible = false
            badgeDrawable.clearNumber()
            binding.bottomNav.removeBadge(R.id.nav_chat)
        }
*//*
        })
*/
    /*    val groupChatBadge = binding.bottomNav.getOrCreateBadge(R.id.nav_group)
        groupChatBadge.isVisible=false
        groupDao.getGroupWithMessages().observe(this, { list->
            val count=list.filter { it.group.unRead!=0 }
            groupChatBadge.isVisible = count.isNotEmpty() //hide if 0
            groupChatBadge.number=count.size
        })*/
        val groupChatBadge = binding.bottomNav.getOrCreateBadge(R.id.nav_group)
        groupChatBadge.isVisible=false
        lifecycleScope.launch {
            groupDao.getGroupWithMessages().collect { list->
                val count=list.filter { it.group.unRead!=0 }
                groupChatBadge.isVisible = count.isNotEmpty() //hide if 0
                groupChatBadge.number=count.size
            }
        }

        lifecycleScope.launch {
            chatUserDao.getChatUserWithMessages().collect { list->
                val count=list.filter { it.user.unRead!=0 }
                badge.isVisible = count.isNotEmpty() //hide if 0
                badge.number=count.size
            }
        }

    }

    private fun setDataInView() {
        try {
            navController = Navigation.findNavController(this, R.id.nav_host_fragment)
            navController.addOnDestinationChangedListener { _, destination, _ ->
                onDestinationChanged()
            }
            val appBarConfiguration = AppBarConfiguration(setOf(R.id.FSingleChatHome))
            binding.toolbar
                .setupWithNavController(navController, appBarConfiguration)
            binding.bottomNav.setOnNavigationItemSelectedListener(onBottomNavigationListener)

            val isNewMessage=intent.action== Constants.ACTION_NEW_MESSAGE
            val isNewGroupMessage=intent.action== Constants.ACTION_GROUP_NEW_MESSAGE
            val userData=intent.getParcelableExtra<ChatUser>(Constants.CHAT_USER_DATA)
            val groupData=intent.getParcelableExtra<Group>(Constants.GROUP_DATA)

            if (preference.isLoggedIn() && navController.isValidDestination(R.id.FLogIn)) {
                if (preference.getUserProfile() == null)
                    navController.navigate(R.id.action_FLogIn_to_FProfile)
                else
                    navController.navigate(R.id.action_FLogIn_to_FSingleChatHome)
            }

            //single chat message clicked
            if (isNewMessage && navController.isValidDestination(R.id.FSingleChatHome)){
                preference.setCurrentUser(userData!!.id)
                val action= FSingleChatHomeDirections.actionFSingleChatToFChat(userData)
                navController.navigate(action)
            }else if (isNewGroupMessage && navController.isValidDestination(R.id.FSingleChatHome)){
                preference.setCurrentGroup(groupData!!.id)
                val action= FSingleChatHomeDirections.actionFSingleChatHomeToFGroupChat(groupData)
                navController.navigate(action)
            }

            if (!preference.isSameDevice())
                Utils.showLoggedInAlert(this, preference, db)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onDestinationChanged() {
        try {
            when {
                navController.isValidDestination(R.id.FSingleChatHome) -> {
                    binding.bottomNav.selectedItemId = R.id.nav_chat
                    showView()
                }
                navController.isValidDestination(R.id.FGroupChatHome) -> {
                    binding.bottomNav.selectedItemId = R.id.nav_group
                    showView()
                }
                navController.isValidDestination(R.id.FSearch) -> {
                    binding.bottomNav.selectedItemId = R.id.nav_search
                    showView()
                }
                navController.isValidDestination(R.id.FMyProfile) -> {
                    binding.bottomNav.selectedItemId = R.id.nav_profile
                    showView()
                }
                else -> {
                    binding.bottomNav.gone()
                    binding.fab.gone()
                    binding.toolbar.gone()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showView() {
        binding.bottomNav.show()
        binding.fab.show()
        binding.toolbar.show()
    }

    private fun isNotSameDestination(destination: Int): Boolean {
        return destination != Navigation.findNavController(this, R.id.nav_host_fragment)
            .currentDestination!!.id
    }

    private val onBottomNavigationListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> {
                    val navOptions =
                        NavOptions.Builder().setPopUpTo(R.id.nav_host_fragment, true).build()
                    if (isNotSameDestination(R.id.FSingleChatHome)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.FSingleChatHome, null, navOptions)
                    }
                    true
                }
                R.id.nav_group -> {
                    if (isNotSameDestination(R.id.FGroupChatHome)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.FGroupChatHome)
                    }
                    true
                }
                R.id.nav_search -> {
                    if (isNotSameDestination(R.id.FSearch)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.FSearch)
                    }
                    true
                }
                else -> {
                    if (isNotSameDestination(R.id.FMyProfile)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.FMyProfile)
                    }
                    true
                }
            }

        }


    fun returnFragment(): Fragment? {
        val navHostFragment: Fragment? =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return navHostFragment?.childFragmentManager?.fragments?.get(0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val navHostFragment = supportFragmentManager.fragments.first() as? NavHostFragment
        if (navHostFragment != null) {
            val childFragments = navHostFragment.childFragmentManager.fragments
            childFragments.forEach { fragment ->
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
       /* val navHostFragment = supportFragmentManager.fragments.first() as? NavHostFragment
        if (navHostFragment != null) {
            val childFragments = navHostFragment.childFragmentManager.fragments
            childFragments.forEach { fragment ->
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }*/
    }

}