package com.decade.practice.model.domain

import com.decade.practice.event.ApplicationEvent

data class Online(val at: Long, val user: User) : ApplicationEvent()
