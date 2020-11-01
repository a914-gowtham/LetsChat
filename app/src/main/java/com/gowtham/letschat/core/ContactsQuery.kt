package com.gowtham.letschat.core

import com.google.firebase.firestore.FirebaseFirestore
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.UserUtils
import timber.log.Timber

interface QueryCompleteListener{
    fun onQueryCompleted(queriedList: ArrayList<UserProfile>)
}

class ContactsQuery(val list: ArrayList<String>,val position: Int,val listener: QueryCompleteListener){

    private val usersCollection = FirebaseFirestore.getInstance().collection("Users")

    fun makeQuery() {
        try {
            usersCollection.whereIn("mobile.number", list).get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val contact = document.toObject(UserProfile::class.java)
                        UserUtils.queriedList.add(contact)
                    }
                    UserUtils.resultCount += 1
                    if(UserUtils.resultCount == UserUtils.totalRecursionCount){
                        listener.onQueryCompleted(UserUtils.queriedList)
                    }
                }
                .addOnFailureListener { exception ->
                    Timber.wtf("Error getting documents: ${exception.message}")
                    UserUtils.resultCount += 1
                    if(UserUtils.resultCount == UserUtils.totalRecursionCount)
                        listener.onQueryCompleted(UserUtils.queriedList)
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}