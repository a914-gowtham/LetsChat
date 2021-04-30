package com.gowtham.letschat.db.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gowtham.letschat.db.data.Message
import com.gowtham.letschat.utils.LogMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleMessage(users: List<Message>)

    @Query("SELECT * FROM Message")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT * FROM Message")
    fun getMessageList(): List<Message>

    @Query("SELECT * FROM Message WHERE `chatUserId`=:chatUserId")
    fun getChatsOfFriend(chatUserId: String): List<Message>

    @Query("SELECT * FROM Message WHERE `chatUserId`=:chatUserId")
    suspend fun getChatsOfFriend2(chatUserId: String): List<Message>

    @Query("SELECT * FROM Message WHERE `to`=:chatUserId OR `from`=:chatUserId")
    fun getMessagesByChatUserId(chatUserId: String): Flow<List<Message>>

    @Query("SELECT * FROM Message  WHERE createdAt=:createdAt")
    suspend fun getMessageById(createdAt: Long): Message?

    @Query("SELECT * FROM Message  WHERE status<3")
    fun getAllNotSeenMessages() : List<Message>

    @Query("DELETE FROM Message  WHERE createdAt=:createdAt")
    suspend fun deleteMessageByCreatedAt(createdAt: Long)

    @Query("DELETE FROM Message WHERE `to`=:userId")
    suspend fun deleteMessagesByUserId(userId: String)

    @Query("DELETE FROM Message")
    fun nukeTable()

}