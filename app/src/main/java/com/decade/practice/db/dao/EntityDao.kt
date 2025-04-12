package com.decade.practice.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.decade.practice.model.Account
import com.decade.practice.model.Chat
import com.decade.practice.model.ChatEvent
import com.decade.practice.model.User

@Dao
interface EntityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChat(chat: Chat)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChats(chat: List<Chat>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(user: List<User>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ChatEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<ChatEvent>)
}