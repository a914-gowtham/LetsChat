package com.gowtham.letschat.db.daos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.gowtham.letschat.db.ChatUserDatabase
import com.gowtham.letschat.db.data.*
import com.gowtham.letschat.getOrAwaitValue
import com.gowtham.letschat.models.UserProfile
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
class MessageDaoTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule= InstantTaskExecutorRule()

    @Inject
    @Named("test_db")
    lateinit var database: ChatUserDatabase

    private lateinit var messageDao: MessageDao

    @Before
    fun setUp() {
        hiltRule.inject()
        messageDao = database.getMessageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_Message() = runBlockingTest {
        val message=Message(2,
             0,0,"fromId","toId","Gowtham","",textMessage = TextMessage(),
             imageMessage = ImageMessage(),audioMessage = AudioMessage(),videoMessage = VideoMessage(),
            fileMessage = FileMessage(),chatUserId = "",chatUsers = ArrayList(),
        )
        messageDao.insertMessage(message)
        val messages=messageDao.getAllMessages().getOrAwaitValue()
        assertThat(messages).contains(message)
    }

    @Test
    fun insert_Multiple_Messages() {
        runBlockingTest {
            val message1=Message(3,
                0,0,"fromId","toId","Gowtham","",textMessage = TextMessage(),
                imageMessage = ImageMessage(),audioMessage = AudioMessage(),videoMessage = VideoMessage(),
                fileMessage = FileMessage(),chatUserId = "",chatUsers = ArrayList(),
            )
            val message2=Message(4,
                0,0,"fromId","toId","Gowtham","",textMessage = TextMessage(),
                imageMessage = ImageMessage(),audioMessage = AudioMessage(),videoMessage = VideoMessage(),
                fileMessage = FileMessage(),chatUserId = "",chatUsers = ArrayList(),
            )
            messageDao.insertMultipleMessage(listOf(message1,message2))
            val messages=messageDao.getAllMessages().getOrAwaitValue()
            assertThat(messages).containsAtLeast(message1,message2)
        }
    }

    @Test
    fun get_Message_ById() {
        runBlockingTest {
            val messageId=5L
            val message=Message(messageId,
                0,0,"fromId","toId","Gowtham","",textMessage = TextMessage(),
                imageMessage = ImageMessage(),audioMessage = AudioMessage(),videoMessage = VideoMessage(),
                fileMessage = FileMessage(),chatUserId = "",chatUsers = ArrayList(),
            )
            messageDao.insertMessage(message)
            val msg=messageDao.getMessageById(messageId)
            assertThat(msg).isNotNull()
        }
    }


    @Test
    fun delete_Message_ById() {
        runBlockingTest {
            val messageId=6L
            val message=Message(messageId,
                0,0,"fromId","toId","Gowtham","",textMessage = TextMessage(),
                imageMessage = ImageMessage(),audioMessage = AudioMessage(),videoMessage = VideoMessage(),
                fileMessage = FileMessage(),chatUserId = "",chatUsers = ArrayList(),
            )
            messageDao.insertMessage(message)
            messageDao.deleteMessageByCreatedAt(messageId)
            val messages=messageDao.getAllMessages().getOrAwaitValue()
            assertThat(messages).doesNotContain(message)
        }
    }
}