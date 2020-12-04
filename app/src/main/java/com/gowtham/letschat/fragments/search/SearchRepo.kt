package com.gowtham.letschat.fragments.search

import com.google.firebase.firestore.CollectionReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepo  @Inject constructor(
    private val usersCollection: CollectionReference,){


}