package com.gowtham.letschat.di

import android.content.Context
import androidx.room.Room
import com.gowtham.letschat.db.ChatUserDatabase
import com.gowtham.letschat.utils.Constants.CHAT_USER_DB_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Singleton
    @Provides
    fun provideChatUserDb(@ApplicationContext context: Context): ChatUserDatabase{
       return Room.databaseBuilder(context,ChatUserDatabase::class.java,
            CHAT_USER_DB_NAME).build()
    }

    @Singleton
    @Provides
    fun provideChatUserDao(db: ChatUserDatabase) = db.getChatUserDao()

    @Singleton
    @Provides
    fun provideMessageDao(db: ChatUserDatabase) = db.getMessageDao()

    @Singleton
    @Provides
    fun provideGroupDao(db: ChatUserDatabase) = db.getGroupDao()

    @Singleton
    @Provides
    fun provideGroupMessageDao(db: ChatUserDatabase) = db.getGroupMessageDao()
}