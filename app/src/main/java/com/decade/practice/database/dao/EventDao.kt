package com.decade.practice.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.ChatIdentifier

@Dao
interface EventDao {

      @Query("SELECT * FROM ChatEvent c WHERE id=:id")
      suspend fun findById(id: String): ChatEvent?

      @Query(
            "SELECT * FROM ChatEvent c " +
                    "WHERE  eventVersion IS NULL " +
                    "ORDER BY receiveTime DESC " +
                    "LIMIT 1"
      )
      suspend fun findUnSent(): ChatEvent?

      @Query(
            "SELECT * FROM ChatEvent c " +
                    "WHERE eventVersion IS NULL " +
                    "AND eventType = 'SEEN' " +
                    "LIMIT 1"
      )
      suspend fun findUnSentSeen(): ChatEvent?

      @Query(
            "SELECT EXISTS(" +
                    "SELECT * FROM ChatEvent c " +
                    "WHERE eventVersion IS NULL " +
                    "LIMIT 1)"
      )
      suspend fun hasUnSent(): Boolean

      @Query(
            "SELECT * FROM ChatEvent c " +
                    "WHERE firstUser=:firstUser " +
                    "AND secondUser=:secondUser " +
                    "ORDER BY eventVersion ASC " +
                    "LIMIT 1 "
      )
      suspend fun last(firstUser: String, secondUser: String): ChatEvent?

      @Query(
            "SELECT * FROM ChatEvent c " +
                    "WHERE receiveTime < :time " +
                    "AND firstUser=:firstUser " +
                    "AND secondUser=:secondUser " +
                    "ORDER BY receiveTime DESC " +
                    "LIMIT :limit "
      )
      suspend fun list(firstUser: String, secondUser: String, time: Long, limit: Int = 40): List<ChatEvent>


      @Query(
            "SELECT COUNT(*) FROM ChatEvent WHERE firstUser=:firstUser AND secondUser=:secondUser AND eventVersion IS NOT NULL"
      )
      suspend fun count(firstUser: String, secondUser: String): Int
}

suspend fun EventDao.last(identifier: ChatIdentifier): ChatEvent? {
      return last(identifier.firstUser, identifier.secondUser)
}

suspend fun EventDao.count(chatIdentifier: ChatIdentifier): Int = count(chatIdentifier.firstUser, chatIdentifier.secondUser)


