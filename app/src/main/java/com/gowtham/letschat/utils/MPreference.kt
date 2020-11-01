package com.gowtham.letschat.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.gowtham.letschat.models.ModelMobile
import com.gowtham.letschat.models.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPreference @Inject constructor(@ApplicationContext private val context: Context) {

    private val UID = "userId"

    private val LOGIN="login"

    private val USER="user"

    private val MOBILE="mobile"

    private val TOKEN="token"

    private val ONLINE_USER="online_user"

    private val ONLINE_GROUP="online_group"

    private val LOGIN_TIME="login_time"

    private val LAST_LOGGED_DEVICE_SAME="last_logged_device_same"

    private val PREFS_FILENAME = "com.gowtham.letschat.prefs"

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_FILENAME, 0)

    private val editor = sharedPreferences.edit()

    private fun storeString(key: String, value: String) {
        editor.run {
            putString(key, value)
            apply()
        }
    }

    private fun storeLong(key: String, value: Long) {
        editor.run {
            putLong(key, value)
            apply()
        }
    }

    private fun storeBoolean(key: String, value: Boolean) =
        editor.run {
            putBoolean(key, value)
            apply()
        }

    private fun getString(key: String) =
        sharedPreferences.getString(key, "")

    fun clearAll() =
        editor?.run {
            clear()
            apply()
        }

    fun setLogin(){
        storeBoolean(LOGIN, true)}

    fun setLastDevice(same: Boolean){
        storeBoolean(LAST_LOGGED_DEVICE_SAME, same)}

    fun setLogInTime(){
        storeLong(LOGIN_TIME,System.currentTimeMillis())
    }

    fun getLogInTime()=
        sharedPreferences.getLong(LOGIN_TIME, 0)

    fun setCurrentUser(id: String){
        storeString(ONLINE_USER, id)
    }

    fun clearCurrentUser() {
        setCurrentUser("")
    }

    fun getOnlineUser(): String {
        return getString(ONLINE_USER) ?: ""
    }

    fun setCurrentGroup(id: String){
        storeString(ONLINE_GROUP, id)
    }

    fun clearCurrentGroup() {
        setCurrentGroup("")
    }

    fun getOnlineGroup(): String {
        return getString(ONLINE_GROUP) ?: ""
    }


    fun isSameDevice()=
        sharedPreferences.getBoolean(LAST_LOGGED_DEVICE_SAME, true)

    fun isLoggedIn()= sharedPreferences.getBoolean(LOGIN, false)

    fun isNotLoggedIn()= !isLoggedIn()

    fun setUid(uid: String) =  storeString(UID, uid)

    fun getUid()= getString(UID)

    fun saveProfile(profile: UserProfile){
        storeString(USER, Gson().toJson(profile))
    }

    fun saveMobile(mobile: ModelMobile){
        storeString(MOBILE, Gson().toJson(mobile))
    }

    fun updatePushToken(token: String){
        storeString(TOKEN, token)
    }

    fun getPushToken() = getString(TOKEN)

    fun getUserProfile(): UserProfile?  {
        val str=getString(USER)
        if (str.isNullOrBlank())
            return null
        return Gson().fromJson(str, UserProfile::class.java)
    }

    fun getMobile(): ModelMobile?  {
        val str=getString(MOBILE)
        if (str.isNullOrBlank())
            return null
        return Gson().fromJson(str, ModelMobile::class.java)
    }

}