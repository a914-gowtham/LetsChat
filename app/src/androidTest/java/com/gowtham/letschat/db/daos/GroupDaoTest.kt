package com.gowtham.letschat.db.daos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.gowtham.letschat.db.ChatUserDatabase
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Group
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
class GroupDaoTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule= InstantTaskExecutorRule()

    @Inject
    @Named("test_db")
    lateinit var database: ChatUserDatabase

    private lateinit var groupDao: GroupDao

    @Before
    fun setUp() {
        hiltRule.inject()
        groupDao = database.getGroupDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_Group() = runBlockingTest {
        val group=Group("testId",members = ArrayList(),profiles = ArrayList())
        groupDao.insertGroup(group)
        val groups=groupDao.getAllGroup().getOrAwaitValue()
        assertThat(groups).contains(group)
    }

    @Test
    fun insert_Multiple_Group() {
        runBlockingTest {
            val group1=Group("testId1",members = ArrayList(),profiles = ArrayList())
            val group2=Group("testId2",members = ArrayList(),profiles = ArrayList())
            groupDao.insertMultipleGroup(listOf(group1,group2))
            val groups=groupDao.getAllGroup().getOrAwaitValue()
            assertThat(groups).containsAtLeast(group1,group2)
        }
    }

    @Test
    fun get_Group_ById() {
        runBlockingTest {
            val newGroup=Group("testId8",members = ArrayList(),profiles = ArrayList())
            groupDao.insertGroup(newGroup)
            val group=groupDao.getGroupById(newGroup.id)
            assertThat(group).isNotNull()
        }
    }

    @Test
    fun delete_Group_ById() {
        runBlockingTest {
            val group=Group("testId5",members = ArrayList(),profiles = ArrayList())
            groupDao.insertGroup(group)
            groupDao.deleteGroupById(group.id)
            val groups=groupDao.getAllGroup().getOrAwaitValue()
            assertThat(groups).doesNotContain(group)
        }
    }

}