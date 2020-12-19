package com.gowtham.letschat.di

import android.content.Context
import androidx.room.Room
import com.gowtham.letschat.db.ChatUserDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Named


@Module
@InstallIn(ApplicationComponent::class)
class TestAppModule {

    @Provides
    @Named("test_db")
    fun provideInMemoryDb(@ApplicationContext context: Context): ChatUserDatabase{
        return Room.inMemoryDatabaseBuilder(
             context,
             ChatUserDatabase::class.java
         ).allowMainThreadQueries().build()
    }
}