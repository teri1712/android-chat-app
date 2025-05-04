package com.decade.practice.session.repository

import com.decade.practice.database.AccountDatabase
import com.decade.practice.database.dao.count
import com.decade.practice.database.dao.last
import com.decade.practice.database.saveEvents
import com.decade.practice.endpoints.HttpContext
import com.decade.practice.endpoints.eventCall
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.presentation.Message
import com.decade.practice.model.presentation.toMessages
import com.decade.practice.session.cache.EventCache
import com.decade.practice.utils.DEFAULT_PAGE_LIMIT
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventRepository @AssistedInject constructor(
      database: AccountDatabase,
      @Assisted private val conversation: Conversation,
      private val eventCache: EventCache,
      remoteRepoFactory: RemoteEventRepository.Factory,
) : ListRepository<ChatEvent, Long> {
      private val remoteRepo: RemoteEventRepository = remoteRepoFactory.create(conversation)

      private val eventDao = database.eventDao()
      override suspend fun list(time: Long?): List<ChatEvent> {
            val chat = conversation.chat
            var events = eventDao.list(
                  chat.identifier.firstUser,
                  chat.identifier.secondUser,
                  time ?: Long.MAX_VALUE,
                  DEFAULT_PAGE_LIMIT
            )
            if (events.size < DEFAULT_PAGE_LIMIT) {
                  val at = events.lastOrNull()
                  events = events + remoteRepo.list(at?.eventVersion)
            }
            return events.apply {
                  forEach {
                        it.conversation = conversation
                  }
                  eventCache.save(conversation, this)
            }
      }


      @AssistedFactory
      interface Factory {
            fun create(conversation: Conversation): EventRepository
      }

}

class RemoteEventRepository @AssistedInject constructor(
      httpContext: HttpContext,
      @Assisted private val conversation: Conversation,
      private val database: AccountDatabase,
) : ListRepository<ChatEvent, Int?> {

      private val retrofit = httpContext.retrofit
      private val eventDao = database.eventDao()

      override suspend fun list(eventVersion: Int?): List<ChatEvent> {
            val identifier = conversation.identifier
            val atVersion = eventVersion
                  ?: eventDao.last(identifier)?.eventVersion
                  ?: (database.accountDao().eventVersion() + 1)
            return withContext(Dispatchers.IO) {
                  retrofit.eventCall().list(
                        identifier.toString(),
                        atVersion - 1
                  )
            }.also { eventList ->
                  if (eventDao.count(identifier) < 100) {
                        database.saveEvents(eventList)
                  }
            }
      }


      @AssistedFactory
      interface Factory {
            fun create(conversation: Conversation): RemoteEventRepository
      }
}


class MessageRepository @AssistedInject constructor(
      @Assisted private val conversation: Conversation,
      eventRepoFactory: EventRepository.Factory,
) : ListRepository<Message, Long> {
      private val eventRepo = eventRepoFactory.create(conversation)

      override suspend fun list(time: Long?): List<Message> =
            conversation.toMessages(eventRepo.list(time))

      @AssistedFactory
      interface Factory {
            fun create(conversation: Conversation): MessageRepository
      }

}