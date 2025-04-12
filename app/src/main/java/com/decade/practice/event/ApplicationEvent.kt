package com.decade.practice.event

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

abstract class ApplicationEvent

interface ApplicationEventListener {
    fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean
    suspend fun onApplicationEvent(applicationEvent: ApplicationEvent)
}

interface ApplicationEventPublisher {
    fun publish(event: ApplicationEvent)
    fun publish(events: List<ApplicationEvent>)
}

@Singleton
class ListenableEventPublisher
@Inject constructor(
    private val components: Set<@JvmSuppressWildcards ApplicationEventListener>,
) : ApplicationEventPublisher {
    private val mainScope = MainScope()
    private val listeners = mutableListOf<ApplicationEventListener>()

    override fun publish(event: ApplicationEvent) {
        mainScope.launch {
            components.forEach { listener ->
                if (listener.supportsEventType(event.javaClass)) {
                    listener.onApplicationEvent(event)
                }
            }
            listeners.forEach { listener ->
                if (listener.supportsEventType(event.javaClass)) {
                    listener.onApplicationEvent(event)
                }
            }
        }
    }

    override fun publish(events: List<ApplicationEvent>) {
        events.forEach {
            publish(it)
        }
    }

    fun register(listener: ApplicationEventListener) {
        listeners.add(listener)
    }

    fun unRegister(listener: ApplicationEventListener) {
        listeners.remove(listener)
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class EventModule {
    @Binds
    abstract fun applicationEventPublisher(publisher: ListenableEventPublisher): ApplicationEventPublisher
}