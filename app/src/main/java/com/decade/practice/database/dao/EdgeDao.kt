package com.decade.practice.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.decade.practice.model.domain.Chat
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.domain.LocalEdge
import com.decade.practice.model.domain.RemoteEdge

@Dao
interface RemoteEdgeDao {

      @Query(
            "SELECT * " +
                    "FROM RemoteEdge e " +
                    "WHERE e.fromfirstUser = :fromFirstUser " +
                    "AND e.fromsecondUser = :fromSecondUser"
      )
      suspend fun edgeFrom(fromFirstUser: String, fromSecondUser: String): RemoteEdge?

      @Query(
            "SELECT * " +
                    "FROM RemoteEdge e " +
                    "WHERE e.tofirstUser = :toFirstUser " +
                    "AND e.tosecondUser = :toSecondUser"
      )
      suspend fun edgeTo(toFirstUser: String, toSecondUser: String): RemoteEdge?

      @Query(
            "SELECT * " +
                    "FROM Chat " +
                    "LEFT JOIN RemoteEdge " +
                    "ON RemoteEdge.fromfirstUser = Chat.firstUser " +
                    "AND RemoteEdge.fromsecondUser = Chat.secondUser " +
                    "WHERE RemoteEdge.fromfirstUser IS NULL"
      )
      suspend fun last(): Conversation

      @Query(
            "SELECT * " +
                    "FROM Chat " +
                    "LEFT JOIN RemoteEdge " +
                    "ON RemoteEdge.tofirstUser = Chat.firstUser " +
                    "AND RemoteEdge.tosecondUser = Chat.secondUser " +
                    "WHERE RemoteEdge.fromfirstUser IS NULL"
      )
      suspend fun head(): Conversation

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun insert(edge: RemoteEdge)

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun insert(edges: List<RemoteEdge>)


      @Delete
      suspend fun delete(edge: RemoteEdge)
}


suspend fun RemoteEdgeDao.edgeFrom(from: Chat): RemoteEdge? {
      val identifier = from.identifier
      return edgeFrom(identifier.firstUser, identifier.secondUser)
}


suspend fun RemoteEdgeDao.edgeTo(to: Chat): RemoteEdge? {
      val identifier = to.identifier
      return edgeTo(identifier.firstUser, identifier.secondUser)
}


@Dao
interface LocalEdgeDao {

      @Query(
            "SELECT * " +
                    "FROM LocalEdge e " +
                    "WHERE e.fromfirstUser = :fromFirstUser " +
                    "AND e.fromsecondUser = :fromSecondUser"
      )
      suspend fun edgeFrom(fromFirstUser: String, fromSecondUser: String): LocalEdge?

      @Query(
            "SELECT * " +
                    "FROM LocalEdge e " +
                    "WHERE e.tofirstUser = :toFirstUser " +
                    "AND e.tosecondUser = :toSecondUser"
      )
      suspend fun edgeTo(toFirstUser: String, toSecondUser: String): LocalEdge?


      @Transaction
      @Query(
            "SELECT * " +
                    "FROM Chat " +
                    "LEFT JOIN LocalEdge " +
                    "ON LocalEdge.fromfirstUser = Chat.firstUser " +
                    "AND LocalEdge.fromsecondUser = Chat.secondUser " +
                    "WHERE LocalEdge.fromfirstUser IS NULL"
      )
      suspend fun last(): Conversation

      @Transaction
      @Query(
            "SELECT * " +
                    "FROM Chat " +
                    "LEFT JOIN LocalEdge " +
                    "ON LocalEdge.tofirstUser = Chat.firstUser " +
                    "AND LocalEdge.tosecondUser = Chat.secondUser " +
                    "WHERE LocalEdge.fromfirstUser IS NULL"
      )
      suspend fun head(): Conversation


      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun insert(edge: LocalEdge)

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun insert(edges: List<LocalEdge>)


      @Delete
      suspend fun delete(edge: LocalEdge)
}

suspend fun LocalEdgeDao.edgeFrom(from: Chat): LocalEdge? {
      val identifier = from.identifier
      return edgeFrom(identifier.firstUser, identifier.secondUser)
}


suspend fun LocalEdgeDao.edgeTo(to: Chat): LocalEdge? {
      val identifier = to.identifier
      return edgeTo(identifier.firstUser, identifier.secondUser)
}


