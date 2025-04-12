package com.decade.practice.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.decade.practice.db.dao.AccountDao
import com.decade.practice.db.dao.ChatDao
import com.decade.practice.db.dao.EntityDao
import com.decade.practice.db.dao.EventDao
import com.decade.practice.db.dao.LocalEdgeDao
import com.decade.practice.db.dao.RemoteEdgeDao
import com.decade.practice.db.dao.UserDao
import com.decade.practice.model.Account
import com.decade.practice.model.Chat
import com.decade.practice.model.ChatEvent
import com.decade.practice.model.LocalEdge
import com.decade.practice.model.RemoteEdge
import com.decade.practice.model.User

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

