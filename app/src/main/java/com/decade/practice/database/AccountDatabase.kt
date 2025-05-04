package com.decade.practice.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.decade.practice.database.dao.AccountDao
import com.decade.practice.database.dao.ChatDao
import com.decade.practice.database.dao.EntityDao
import com.decade.practice.database.dao.EventDao
import com.decade.practice.database.dao.LocalEdgeDao
import com.decade.practice.database.dao.RemoteEdgeDao
import com.decade.practice.database.dao.UserDao
import com.decade.practice.model.domain.Account
import com.decade.practice.model.domain.Chat
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.LocalEdge
import com.decade.practice.model.domain.RemoteEdge
import com.decade.practice.model.domain.User

@Database(
      entities = [Chat::class, Account::class, User::class, ChatEvent::class, LocalEdge::class, RemoteEdge::class
      ], version = 1
)
abstract class AccountDatabase : RoomDatabase() {

      abstract fun remoteEdgeDao(): RemoteEdgeDao
      abstract fun localEdgeDao(): LocalEdgeDao
      abstract fun accountDao(): AccountDao
      abstract fun chatDao(): ChatDao
      abstract fun entityDao(): EntityDao
      abstract fun eventDao(): EventDao
      abstract fun userDao(): UserDao
}


suspend fun AccountDatabase.saveChat(entity: Chat) {
      entityDao().insertChat(entity)
}

suspend fun AccountDatabase.saveEvent(entity: ChatEvent) {
      entityDao().insertEvent(entity)
}

suspend fun AccountDatabase.insertUser(entity: User) {
      entityDao().insertUser(entity)
}

suspend fun AccountDatabase.save(entity: Account) {
      entityDao().insert(entity)
}

suspend fun AccountDatabase.saveChats(entity: List<Chat>) {
      entityDao().insertChats(entity)
}

suspend fun AccountDatabase.saveEvents(entity: List<ChatEvent>) {
      entityDao().insertEvents(entity)
}

suspend fun AccountDatabase.saveEdges(localEdges: List<LocalEdge> = emptyList(), remoteEdges: List<RemoteEdge> = emptyList()) {
      localEdgeDao().insert(localEdges)
      remoteEdgeDao().insert(remoteEdges)
}

suspend fun AccountDatabase.saveUsers(entity: List<User>) {
      entityDao().insertUsers(entity)
}

suspend fun AccountDatabase.saveUser(entity: User) {
      entityDao().insertUser(entity)
}

suspend fun AccountDatabase.getAccount() = accountDao().get()

