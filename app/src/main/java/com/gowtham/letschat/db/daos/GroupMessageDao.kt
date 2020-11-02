package com.gowtham.letschat.db.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.db.data.GroupWithMessages
import com.gowtham.letschat.db.data.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: GroupMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleMessage(users: List<GroupMessage>)

    @Query("SELECT * FROM GroupMessage")
    fun getAllMessages(): LiveData<List<GroupMessage>>

    @Query("SELECT * FROM GroupMessage")
    fun getMessageList(): List<GroupMessage>

    @Query("SELECT * FROM GroupMessage WHERE groupId=:groupId")
    fun getChatsOfGroupList(groupId: String): List<GroupMessage>

    @Query("SELECT * FROM GroupMessage WHERE groupId=:groupId")
    fun getChatsOfGroup(groupId: String): Flow<List<GroupMessage>>

    @Query("DELETE FROM GroupMessage  WHERE createdAt=:createdAt")
    suspend fun deleteMessageByCreatedAt(createdAt: Long)

    @Query("DELETE FROM GroupMessage  WHERE groupId=:groupId")
    suspend fun deleteMessagesByGroupId(groupId: String)

    @Query("DELETE FROM GroupMessage")
    fun nukeTable()
}