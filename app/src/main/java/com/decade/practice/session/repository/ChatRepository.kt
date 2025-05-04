package com.decade.practice.session.repository

import androidx.room.withTransaction
import com.decade.practice.database.AccountDatabase
import com.decade.practice.database.dao.edgeFrom
import com.decade.practice.database.getAccount
import com.decade.practice.database.saveChats
import com.decade.practice.database.saveEvents
import com.decade.practice.database.saveUsers
import com.decade.practice.endpoints.HttpContext
import com.decade.practice.endpoints.chatCall
import com.decade.practice.model.domain.Chat
import com.decade.practice.model.domain.ChatSnapshot
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.domain.LocalEdge
import com.decade.practice.model.domain.RemoteEdge
import com.decade.practice.session.AccountScope
import com.decade.practice.utils.DEFAULT_PAGE_LIMIT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AccountScope
class ChatRepository @Inject constructor(
      httpContext: HttpContext,
      private val database: AccountDatabase,
) : ListRepository<ChatSnapshot, Conversation> {
      private val retrofit = httpContext.retrofit

      private val chatDao = database.chatDao()
      private val localEdgeDao = database.localEdgeDao()
      private val remoteEdgeDao = database.remoteEdgeDao()

      private suspend fun localList(index: Conversation, limit: Int) = database.withTransaction {
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

      override suspend fun list(index: Conversation?): List<ChatSnapshot> = withContext(Dispatchers.IO) {
            val at = index ?: localEdgeDao.head()
            val snapshots = localList(at, DEFAULT_PAGE_LIMIT)
            if (snapshots.size < DEFAULT_PAGE_LIMIT) {
                  expandLocal()
                  return@withContext localList(at, DEFAULT_PAGE_LIMIT)
            }
            return@withContext snapshots
      }

      private suspend fun prepareHttpQuery() = database.withTransaction {
            val account = database.getAccount()
            val last = remoteEdgeDao.last().chat
            val currentVersion = account.syncContext.eventVersion
            return@withTransaction Pair(currentVersion, last.identifier)
      }

      private suspend fun resolveExpansion(chatList: List<Chat>) {
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
      }

      private suspend fun expandLocal() {
            val query = prepareHttpQuery()
            val atVersion = query.first
            val atChat = query.second
            val response = retrofit.chatCall().list(atVersion, atChat)

            val list = response.subList(1, response.size)
            if (list.isEmpty())
                  return

            val chatList = list.map { it.conversation.chat }
            val userList = list.map { it.conversation.partner }
            val eventList = list.flatMap { it.eventList }
            database.withTransaction {

                  val account = database.getAccount()
                  if (account.syncContext.eventVersion == atVersion) {
                        resolveExpansion(chatList)
                        eventList.forEach { event ->
                              event.committed = true
                              event.receiveTime = event.createdTime
                        }
                        database.saveUsers(userList)
                        database.saveEvents(eventList)
                  }
            }
      }
}
