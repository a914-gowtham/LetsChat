package com.gowtham.letschat.db.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gowtham.letschat.db.data.ChatUserWithMessages
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.db.data.GroupWithMessages
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleGroup(listOfGroup: List<Group>)

    @Query("SELECT * FROM `Group` ORDER BY id ASC")
    fun getAllGroup(): LiveData<List<Group>>

    @Query("SELECT * FROM `Group` ORDER BY id ASC")
    fun getGroupList(): List<Group>

    @Query("SELECT * FROM `Group`  WHERE id=:groupId")
    fun getGroupById(groupId: String): Group?

    @Query("DELETE FROM `Group` WHERE id=:groupId")
    suspend fun deleteGroupById(groupId: String)

    @Query("DELETE FROM `Group`")
    fun nukeTable()

    @Transaction
    @Query("SELECT * FROM `Group`")
    fun getGroupWithMessagesList(): List<GroupWithMessages>

    @Transaction
    @Query("SELECT * FROM `Group`")
    fun getGroupWithMessages(): Flow<List<GroupWithMessages>>

}