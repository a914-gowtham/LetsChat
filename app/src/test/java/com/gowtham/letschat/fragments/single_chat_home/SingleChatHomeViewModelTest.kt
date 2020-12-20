package com.gowtham.letschat.fragments.single_chat_home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gowtham.letschat.db.DbRepositoryTest
import com.gowtham.letschat.utils.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SingleChatHomeViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SingleChatHomeViewModel

    @Before
    fun setUp(){

    }

    fun insert_ChatUser(){

    }


}