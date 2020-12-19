package com.gowtham.letschat.db.daos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.gowtham.letschat.db.ChatUserDatabase
import com.gowtham.letschat.db.data.*
import com.gowtham.letschat.getOrAwaitValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Named

@ExperimentalCoroutinesApi
@SmallTest
@HiltAndroidTest
class GroupMessageDaoTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule= InstantTaskExecutorRule()

    @Inject
    @Named("test_db")
    lateinit var database: ChatUserDatabase

    private lateinit var groupMessageDao: GroupMessageDao

    @Before
    fun setUp() {
        hiltRule.inject()
        groupMessageDao = database.getGroupMessageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_Message() = runBlockingTest {
        val message=GroupMessage(1,"testGroupId","fromMe", ArrayList(),
            "gowtham","",textMessage = TextMessage(),
             imageMessage = ImageMessage(),audioMessage = AudioMessage(),videoMessage = VideoMessage(),
            fileMessage = FileMessage(),deliveryTime = ArrayList(),seenTime = ArrayList(),status = ArrayList()
        )
        groupMessageDao.insertMessage(message)
        val messages=groupMessageDao.getAllMessages().getOrAwaitValue()
        assertThat(messages).contains(message)
    }

    @Test
    fun insert_Multiple_Messages() {
        runBlockingTest {
            val message1=GroupMessage(2,"testGroupId1","fromMe", ArrayList(),
                "gowtham","",textMessage = TextMessage(),
                imageMessage = ImageMessage(),audioMessage = AudioMessage(),videoMessage = VideoMessage(),
                fileMessage = FileMessage(),deliveryTime = ArrayList(),seenTime = ArrayList(),status = ArrayList()
            )

            val message2=GroupMessage(3,"testGroupId2","fromMe", ArrayList(),
                "gowtham","",textMessage = TextMessage(),
                imageMessage = ImageMessage(),audioMessage = AudioMessage(),videoMessage = VideoMessage(),
                fileMessage = FileMessage(),deliveryTime = ArrayList(),seenTime = ArrayList(),status = ArrayList()
            )
            groupMessageDao.insertMultipleMessage(listOf(message1,message2))
            val messages=groupMessageDao.getAllMessages().getOrAwaitValue()
            assertThat(messages).containsAtLeast(message1,message2)
        }
    }

}