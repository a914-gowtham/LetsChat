package com.gowtham.letschat.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gowtham.letschat.db.data.ChatUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStorePreference @Inject constructor(@ApplicationContext private val context: Context){

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "search_results")

    fun storeList(key: String,value: List<ChatUser>){
        CoroutineScope(Dispatchers.IO).launch {
           val dataStoreKey= stringPreferencesKey(key)
            context.dataStore.edit {
                it[dataStoreKey]=Json.encodeToString(value)
            }
        }
    }

    fun getList(): Flow<String>{
        val listKey = stringPreferencesKey(Constants.KEY_LAST_QUERIED_LIST)
        return context.dataStore.data
            .map { preferences ->
                // No type safety.
                preferences[listKey] ?: ""
            }
    }

}