package com.gowtham.letschat.fragments.search

import androidx.paging.PagingSource
import coil.network.HttpException
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.gowtham.letschat.models.UserProfile
import com.gowtham.letschat.utils.UserUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.CountDownLatch
import kotlin.system.measureTimeMillis

private const val UNSPLASH_STARTING_PAGE_INDEX = 0

class UnsplashPagingSource(
    private val userCollection: CollectionReference,
    private val query: String
) : PagingSource<Int, UserProfile>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserProfile> {
        val position = params.key ?: UNSPLASH_STARTING_PAGE_INDEX
        var result: LoadResult<Int, UserProfile>?=null
        Timber.v("Load called position $position")
        withContext(Dispatchers.IO){
            val countDownLatch = CountDownLatch(1)
            val query=  userCollection
                .whereEqualTo("userName",query)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(20)
            query.get()
                .addOnSuccessListener {
                    val users= mutableListOf<UserProfile>()
                    for (document in it.documents) {
                        val contact = document.toObject(UserProfile::class.java)!!
                        users.add(contact)
                    }
                    Timber.v("List size ${users.size}")
                    result=  LoadResult.Page(
                        data = users,
                        prevKey = if (position == UNSPLASH_STARTING_PAGE_INDEX) null else position - 1,
                        nextKey = if (users.isEmpty()) null else position + 1
                    )
                    countDownLatch.countDown()
                }.addOnFailureListener {
                    result= LoadResult.Error(it)
                    countDownLatch.countDown()
                }
            countDownLatch.await()
        }
        return result!!
    }
}