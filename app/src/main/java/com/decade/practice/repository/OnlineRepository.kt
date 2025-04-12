package com.decade.practice.repository

import com.decade.practice.session.AccountScope
import com.decade.practice.model.Online
import com.decade.practice.net.HttpContext
import com.decade.practice.net.api.onlineCall
import javax.inject.Inject

@AccountScope
class OnlineRepository @Inject constructor(
    httpContext: HttpContext,
) : AbstractObservableRepository<Online, String>() {
    private val retrofit = httpContext.retrofit
    override suspend fun list(): List<Online> =
        retrofit.onlineCall().list().also { list ->
            notifyObservers(list)
        }

    override suspend fun list(offset: String, limit: Int) = list()

    override suspend fun get(username: String): Online =
        retrofit.onlineCall().get(username).also { online ->
            notifyObservers(online)
        }
}