package com.gowtham.letschat.db.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.ChatUserWithMessages
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: ChatUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleUser(users: List<ChatUser>)

    @Query("SELECT * FROM ChatUser ORDER BY localName ASC")
    fun getAllChatUser(): LiveData<List<ChatUser>>

    @Query("SELECT * FROM ChatUser ORDER BY localName ASC")
    fun getChatUserList(): List<ChatUser>

    @Query("SELECT * FROM ChatUser  WHERE id=:id")
    fun getChatUserById(id: String): ChatUser?

    @Query("SELECT * FROM ChatUser  WHERE id=:id")
    suspend fun getChatUserById2(id: String): ChatUser?

    @Query("DELETE FROM ChatUser WHERE id=:userId")
    suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM ChatUser")
    fun nukeTable()

    @Transaction
    @Query("SELECT * FROM ChatUser")
    fun getChatUserWithMessages(): Flow<List<ChatUserWithMessages>>

    @Transaction
    @Query("SELECT * FROM ChatUser")
    fun getChatUserWithMessagesList(): List<ChatUserWithMessages>
}