package com.decade.practice.session.repository

import com.decade.practice.endpoints.HttpContext
import com.decade.practice.endpoints.onlineCall
import com.decade.practice.model.domain.Online
import com.decade.practice.session.AccountScope
import com.decade.practice.session.cache.SimpleCache
import com.decade.practice.utils.ONE_MINUTE_SECONDS
import java.time.Instant
import javax.inject.Inject

@AccountScope
class OnlineRepository @Inject constructor(
      httpContext: HttpContext,
) : Repository<Online, String> {

      private val cache = SimpleCache<String, Online>()
      private val retrofit = httpContext.retrofit

      override suspend fun list(i: String?): List<Online> =
            retrofit.onlineCall().list().apply {
                  forEach { online ->
                        cache.save(online.user.username, online)
                  }
            }

      override suspend fun get(username: String): Online {
            val online = cache.get(username)
            if (online != null) {
                  val diff = Instant.now().epochSecond - online.at
                  if (diff < 2 * ONE_MINUTE_SECONDS)
                        return online
                  //TODO: evict
            }

            return retrofit.onlineCall().get(username).apply {
                  cache.save(username, this)
            }
      }
}