package com.gowtham.letschat.utils

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.gowtham.letschat.R
import com.gowtham.letschat.db.ChatUserDatabase
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.models.Country
import com.gowtham.letschat.models.UserStatus
import java.text.SimpleDateFormat

object Utils {

    private const val PERMISSION_REQ_CODE = 114

  /*  private const val MIN: Long=1000 * 60
    private const val HOUR= MIN * 60
    private const val DAY= HOUR* 24
    private const val WEEK= DAY * 7
    private const val MONTH= WEEK * 4
    private const val YEAR= MONTH * 12*/

    fun getDefaultCountry() = Country("IN", "India", "+91", "INR")

    fun clearNull(str: String?) = str?.trim() ?: ""

    @Suppress("DEPRECATION")
    fun isNetConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            (capabilities != null &&
                    ((capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            (activeNetworkInfo != null && activeNetworkInfo.isConnected)
        }
    }

    fun isNoInternet(context: Context) = !isNetConnected(context)

    fun getColor(context: Context, color: Int): Int {
        return ContextCompat.getColor(context, color)
    }

    fun checkPermission(context: Fragment,
                        vararg permissions: String,reqCode: Int= PERMISSION_REQ_CODE): Boolean {
        var allPermitted = false
        for (permission in permissions) {
            allPermitted = (ContextCompat.checkSelfPermission(context.requireContext(), permission)
                    == PackageManager.PERMISSION_GRANTED)
            if (!allPermitted) break
        }
        if (allPermitted) return true
        context.requestPermissions(
            permissions,
            reqCode
        )
        return false
    }

    fun getGSONObj(): Gson {
        return GsonBuilder().create()
    }

/*    fun <T> fromGson(json: String?, className: Class<T>?): T {
        return getGSONObj().fromJson(json, className)
    }*/

    fun isPermissionOk(vararg results: Int): Boolean {
        var isAllGranted = true
        for (result in results) {
            if (PackageManager.PERMISSION_GRANTED != result) {
                isAllGranted = false
                break
            }
        }
        return isAllGranted
    }

    fun startNewActivity(activity: Activity, className: Class<*>?) {
        val intent = Intent(activity, className)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    fun askContactPermission(context: Fragment): Boolean {
        return checkPermission(
            context, android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS
        )
    }

    fun isContactPermissionOk(context: Context): Boolean{
        return (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_CONTACTS
                )
                        == PackageManager.PERMISSION_GRANTED)
    }

    fun showLoggedInAlert(
        context: Activity, preference: MPreference,
        db: ChatUserDatabase) {
        try {
            val dialog = Dialog(context)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.alert_dialog)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.findViewById<TextView>(R.id.txt_log_out).setOnClickListener {
                dialog.dismiss()
                UserUtils.logOut(context, preference, db)
            }
            dialog.show()
        } catch (e: Exception) {
        }
    }

   /* fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
*/
    fun setOnlineStatus(txtView: TextView, status: UserStatus, uId: String) {
        txtView.visibility= View.VISIBLE
        txtView.text= when {
            status.typing_status=="typing" && uId==status.chatuser -> {
                "typing..."
            }
            status.status=="online" -> {
                "online"
            }
            status.last_seen>0L -> {
                "last seen ${getLastSeen(status.last_seen)}"
            }
            else -> {
                "..."
            }
        }
    }

    fun createBuilder(
        context: Context,
        manager: NotificationManagerCompat,
        isSummary: Boolean = false
    ): NotificationCompat.Builder {
        val channelId = context.packageName
        val notBuilder = NotificationCompat.Builder(context, channelId)
        notBuilder.setSmallIcon(R.drawable.ic_stat_name)
        notBuilder.setAutoCancel(true)
        notBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notBuilder.setDefaults(Notification.DEFAULT_ALL)
        notBuilder.priority = NotificationCompat.PRIORITY_HIGH
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        notBuilder.setSound(soundUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance=if (isSummary) NotificationManager.IMPORTANCE_HIGH else
                NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                channelId, context.getString(R.string.txt_notifications),
                importance
            )
            channel.importance =importance
            channel.shouldShowLights()
            channel.lightColor = Color.BLUE
            channel.canBypassDnd()
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            channel.setSound(soundUri, audioAttributes)
            channel.description = context.getString(R.string.txt_not_description)
            notBuilder.setChannelId(channelId)
            manager.createNotificationChannel(channel)
        }
        return notBuilder
    }

    fun returnNManager(context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }

    fun removeNotification(context: Context){
        val manager= returnNManager(context)
        manager.cancelAll()
    }

    fun removeNotificationById(context: Context, id: Int){
        val manager= returnNManager(context)
        manager.cancel(id)
    }

    fun getGroupName(groupId: String) =
        groupId.substring(0, groupId.lastIndexOf("_"))

    fun closeKeyBoard(activity: Activity){
        val view=activity.currentFocus
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    fun showSoftKeyboard(activity: Activity, view: View) {
        if (view.requestFocus()) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun myMsgStatus(myUserId: String, msg: GroupMessage): Int{
        val indexOfMine=myIndexOfStatus(myUserId, msg)
        return msg.status[indexOfMine]
    }

    fun myIndexOfStatus(myUserId: String, msg: GroupMessage): Int{
        return msg.to.indexOf(myUserId)
    }

    @SuppressLint("SimpleDateFormat")
    fun getTime(sentTime: Long): String{
        val currentTime=System.currentTimeMillis()
        val dayCount = (currentTime - sentTime)/(24 * 60 * 60 * 1000)
        val calender= java.util.Calendar.getInstance()
        calender.timeInMillis=sentTime
        val date=calender.time
        return when{
            dayCount> 1L -> {
                //DD/MM/YYYY format
                SimpleDateFormat("dd/MMM/yyyy").format(date)
            }
            dayCount==1L -> {
                "Yesterday"
            }
            else->{
                //hh:mm aa
                SimpleDateFormat("hh:mm aa").format(date)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getLastSeen(lastSeen: Long): String{
        val currentTime=System.currentTimeMillis()
        val dayCount = (currentTime - lastSeen)/(24 * 60 * 60 * 1000)
        val calender= java.util.Calendar.getInstance()
        calender.timeInMillis=lastSeen
        val date=calender.time
        return when{
            dayCount> 1L -> {
                //DD/MM/YYYY format
                SimpleDateFormat("dd/MMM/yyyy").format(date)
            }
            dayCount==1L -> {
                "yesterday ${SimpleDateFormat("hh:mm aa").format(date)}"
            }
            else->{
                //hh:mm aa
                "today ${SimpleDateFormat("hh:mm aa").format(date)}"
            }
        }
    }

    fun edtValue(edtMsg: EditText): String {
      return edtMsg.text!!.trim().toString()
    }
}

