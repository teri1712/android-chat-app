package com.decade.practice.repository

import com.decade.practice.db.AccountDatabase
import com.decade.practice.db.dao.count
import com.decade.practice.db.dao.last
import com.decade.practice.db.saveEvents
import com.decade.practice.model.Chat
import com.decade.practice.model.ChatEvent
import com.decade.practice.model.Conversation
import com.decade.practice.net.HttpContext
import com.decade.practice.net.api.eventCall
import com.decade.practice.view.viewmodel.Message
import com.decade.practice.view.viewmodel.toMessages
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EventRepository @Inject constructor(
    database: AccountDatabase,
    private val remoteRepo: RemoteEventRepository,
) : AbstractObservableRepository<ChatEvent, Pair<Conversation, Long>>() {

    private val eventDao = database.eventDao()
    override suspend fun list(index: Pair<Conversation, Long>, limit: Int): List<ChatEvent> {
        val conversation = index.first
        val chat = conversation.chat
        val time = index.second
        var events = eventDao.list(
            chat.identifier.firstUser,
            chat.identifier.secondUser,
            time,
            limit
        )
        if (events.size < limit) {
            val at = events.lastOrNull()
            events = events + remoteRepo.list(Pair(chat, at?.eventVersion))
        }
        return events.apply {
            forEach {
                it.conversation = conversation
            }
            notifyObservers(this)
        }
    }

}

class RemoteEventRepository @Inject constructor(
    httpContext: HttpContext,
    private val database: AccountDatabase,
) : Repository<ChatEvent, Pair<Chat, Int?>> {

    private val retrofit = httpContext.retrofit
    private val eventDao = database.eventDao()

    override suspend fun list(index: Pair<Chat, Int?>, limit: Int): List<ChatEvent> {
        val identifier = index.first.identifier
        val eventVersion = index.second
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


}


class MessageRepository @AssistedInject constructor(
    @Assisted private val conversation: Conversation,
    private val eventRepo: EventRepository,
) : Repository<Message, Long> {
    override suspend fun list(time: Long, limit: Int): List<Message> =
        conversation.toMessages(eventRepo.list(Pair(conversation, time), limit))

    override suspend fun list(): List<Message> =
        conversation.toMessages(eventRepo.list(Pair(conversation, Long.MAX_VALUE)))

    @AssistedFactory
    interface Factory {
        fun create(conversation: Conversation): MessageRepository
    }

}