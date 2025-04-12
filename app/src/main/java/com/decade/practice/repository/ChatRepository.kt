package com.decade.practice.repository

import androidx.room.withTransaction
import com.decade.practice.session.AccountScope
import com.decade.practice.db.AccountDatabase
import com.decade.practice.db.dao.edgeFrom
import com.decade.practice.db.getAccount
import com.decade.practice.db.saveChats
import com.decade.practice.db.saveEvents
import com.decade.practice.db.saveUsers
import com.decade.practice.model.Chat
import com.decade.practice.model.ChatSnapshot
import com.decade.practice.model.Conversation
import com.decade.practice.model.LocalEdge
import com.decade.practice.model.RemoteEdge
import com.decade.practice.net.HttpContext
import com.decade.practice.net.api.chatCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AccountScope
class ChatRepository @Inject constructor(
    private val remoteRepo: ChatRemoteRepository,
    private val database: AccountDatabase,
) : AbstractObservableRepository<ChatSnapshot, Conversation>() {

    private val chatDao = database.chatDao()
    private val localEdgeDao = database.localEdgeDao()
    private val remoteEdgeDao = database.remoteEdgeDao()

    private suspend fun localList(index: Conversation, limit: Int) = withContext(Dispatchers.IO) {
        database.withTransaction {
            var current = index
            val conversations = mutableListOf(current)
            var count = limit
            while (--count >= 0) {
                val next = localEdgeDao.edgeFrom(current.chat) ?: break
                current = chatDao.getConversation(next.to)
                conversations.add(current)
            }
            return@withTransaction conversations.map { conversation ->
                chatDao.getSnapshot(conversation)
            }
        }
    }.also { snapshots ->
        notifyObservers(snapshots)
    }

    override suspend fun list(index: Conversation, limit: Int): List<ChatSnapshot> {
        val snapshots = localList(index, limit)
        if (snapshots.size < limit) {
            expandLocal()
            return localList(index, limit)
        }
        return snapshots
    }

    private suspend fun expandLocal() = withContext(Dispatchers.IO) {
        val query = database.withTransaction {
            val account = database.getAccount()
            val last = remoteEdgeDao.last().chat
            val currentVersion = account.syncContext.eventVersion
            return@withTransaction Pair(currentVersion, last)
        }
        val atVersion = query.first
        val list = remoteRepo.list(query)

        if (list.isEmpty())
            return@withContext

        val chatList = list.map {
            it.conversation.chat
        }
        val userList = list.map { it.conversation.partner }
        val eventList = list.flatMap { it.eventList }
        eventList.forEach { event ->
            event.committed = true
            event.receiveTime = event.createdTime
        }
        database.withTransaction {
            val account = database.getAccount()
            if (account.syncContext.eventVersion == atVersion) {
                database.saveChats(chatList)
                var prev = remoteEdgeDao.last().chat
                chatList.forEach { current ->
                    remoteEdgeDao.insert(RemoteEdge(prev, current))
                    prev = current
                }

                prev = localEdgeDao.last().chat
                chatList.forEach { current ->
                    if (!chatDao.hasChat(current.identifier)) {

                        localEdgeDao.insert(LocalEdge(prev, current))
                        prev = current
                    }
                }
                database.saveUsers(userList)
                database.saveEvents(eventList)
            }
        }
    }

    override suspend fun list(): List<ChatSnapshot> {
        val head = localEdgeDao.head()
        return localList(head, 10)
    }
}


@AccountScope
class ChatRemoteRepository @Inject constructor(
    httpContext: HttpContext,
) : Repository<ChatSnapshot, Pair<Int, Chat>> {
    private val retrofit = httpContext.retrofit

    override suspend fun list(index: Pair<Int, Chat>, limit: Int): List<ChatSnapshot> {
        val chat = index.second
        val atVersion = index.first

        val result = retrofit.chatCall().list(atVersion, chat.identifier)
        assert(result.isNotEmpty() && result.first().conversation.chat == chat)

        return result.subList(1, result.size)
    }

}


@AccountScope
class ConversationRepository @Inject constructor(
    private val chatRepo: ChatRepository
) : Repository<Conversation, Conversation> {

    override suspend fun list(index: Conversation, limit: Int): List<Conversation> =
        chatRepo.list(index, limit).map { snapshot ->
            snapshot.conversation
        }

    override suspend fun list(): List<Conversation> =
        chatRepo.list().map { snapshot ->
            snapshot.conversation
        }
}
