package com.decade.practice.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.decade.practice.model.ChatEvent
import com.decade.practice.model.ChatIdentifier
import com.decade.practice.model.ChatSnapshot
import com.decade.practice.model.Conversation
import java.util.LinkedList

@Dao
interface ChatDao {

    @Query("SELECT eventVersion FROM Account")
    suspend fun eventVersion(): Int

    @Transaction
    @Query(
        "SELECT * FROM Chat " +
                "WHERE firstUser = :firstUser " +
                "AND secondUser = :secondUser "
    )
    suspend fun getConversation(firstUser: String, secondUser: String): Conversation

    @Query(
        "SELECT EXISTS (SELECT * FROM Chat " +
                "WHERE firstUser = :firstUser " +
                "AND secondUser = :secondUser)"
    )
    suspend fun hasChat(firstUser: String, secondUser: String): Boolean

    suspend fun hasChat(identifier: ChatIdentifier): Boolean =
        hasChat(identifier.firstUser, identifier.secondUser)

    suspend fun getConversation(identifier: ChatIdentifier): Conversation {
        return getConversation(identifier.firstUser, identifier.secondUser)
    }


    @Query(
        "SELECT * FROM ChatEvent " +
                "WHERE firstUser=:firstUser " +
                "AND secondUser = :secondUser " +
                "ORDER BY receiveTime DESC LIMIT 10"
    )
    suspend fun listEvent(firstUser: String, secondUser: String): List<ChatEvent>

    suspend fun listEvent(identifier: ChatIdentifier): List<ChatEvent> {
        return listEvent(identifier.firstUser, identifier.secondUser)
    }

    suspend fun getSnapshot(conversation: Conversation): ChatSnapshot {
        val eventList = listEvent(conversation.identifier).apply {
            forEach { chatEvent ->
                chatEvent.conversation = conversation
            }
        }
        return ChatSnapshot(conversation, LinkedList(eventList))
    }


}

