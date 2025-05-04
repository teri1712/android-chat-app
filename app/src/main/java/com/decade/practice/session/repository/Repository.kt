package com.decade.practice.session.repository


interface ListRepository<out T, in I> {
      suspend fun list(index: I? = null): List<T>
}

interface GetRepository<out T, in I> {
      suspend fun get(index: I): T?
}

interface Repository<out T, in I> : ListRepository<T, I>, GetRepository<T, I> {}
