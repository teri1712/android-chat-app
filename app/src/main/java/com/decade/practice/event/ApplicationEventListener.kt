package com.decade.practice.event

interface ApplicationEventListener {
      fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean
      suspend fun onApplicationEvent(applicationEvent: ApplicationEvent)
}