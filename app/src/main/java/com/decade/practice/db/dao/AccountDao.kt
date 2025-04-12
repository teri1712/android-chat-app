package com.decade.practice.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.decade.practice.model.Account

@Dao
interface AccountDao {

    @Query("SELECT * FROM Account")
    suspend fun get(): Account

    @Query("SELECT eventVersion FROM Account")
    suspend fun eventVersion(): Int

    @Query("SELECT * FROM Account")
    fun getBlocking(): Account

    @Update
    fun save(account: Account)

    @Query("UPDATE Account SET accessToken=:accessToken, expiresIn=:expiresIn")
    fun save(accessToken: String, expiresIn: Long)

}
