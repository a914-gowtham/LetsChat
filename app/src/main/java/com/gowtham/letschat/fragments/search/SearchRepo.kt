package com.gowtham.letschat.fragments.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.liveData
import com.google.firebase.firestore.CollectionReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepo  @Inject constructor(
    private val usersCollection: CollectionReference,){

    fun getSearchResults(query: String) =
        Pager(
            config = PagingConfig(
                pageSize = 20,
                maxSize = 100,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { UnsplashPagingSource(usersCollection,query) }
        ).liveData
}