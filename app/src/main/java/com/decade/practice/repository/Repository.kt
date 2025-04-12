package com.decade.practice.repository

import com.decade.practice.authentication.AuthenticationException
import com.decade.practice.session.Session


interface Repository<out T, in I> {
    suspend fun get(index: I): T = TODO("Operation is not supported")
    suspend fun list(index: I, limit: Int = 10): List<T> = TODO("Operation is not supported")
    suspend fun list(): List<T> = TODO("Operation is not supported")
}

fun interface RepositoryObserver<T> {
    fun onLoaded(item: T)
}

interface ObservableRepository<T, I> : Repository<T, I> {
    fun observe(observer: RepositoryObserver<T>)
    fun unObserve(observer: RepositoryObserver<T>)
}

abstract class AbstractObservableRepository<T, I> : ObservableRepository<T, I> {

    private val observers = mutableSetOf<RepositoryObserver<T>>()

    override fun observe(observer: RepositoryObserver<T>) {
        observers.add(observer)
    }

    override fun unObserve(observer: RepositoryObserver<T>) {
        observers.remove(observer)
    }

    protected fun notifyObservers(item: T) {
        observers.forEach { observer ->
            observer.onLoaded(item)
        }
    }

    protected fun notifyObservers(items: List<T>) {
        items.forEach { item ->
            notifyObservers(item)
        }
    }

}


interface AccountRepository {

    val currentSession: Session?
    val hasSession: Boolean
        get() = currentSession != null

    @Throws(AuthenticationException::class)
    suspend fun logIn(username: String, password: String): Session

    @Throws(AuthenticationException::class)
    suspend fun logIn(accessToken: String): Session
    suspend fun logOut()
}