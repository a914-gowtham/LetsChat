package com.gowtham.letschat.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gowtham.letschat.db.daos.ChatUserDao
import com.gowtham.letschat.db.daos.GroupDao
import com.gowtham.letschat.db.daos.GroupMessageDao
import com.gowtham.letschat.db.daos.MessageDao
import com.gowtham.letschat.db.data.ChatUser
import com.gowtham.letschat.db.data.Group
import com.gowtham.letschat.db.data.GroupMessage
import com.gowtham.letschat.db.data.Message

@Database(entities = [ChatUser::class, Message::class,Group::class,GroupMessage::class],
    version = 1, exportSchema = false)
@TypeConverters(TypeConverter::class)
abstract class ChatUserDatabase : RoomDatabase()  {
    abstract fun getChatUserDao(): ChatUserDao
    abstract fun getMessageDao(): MessageDao
    abstract fun getGroupDao(): GroupDao
    abstract fun getGroupMessageDao(): GroupMessageDao
}