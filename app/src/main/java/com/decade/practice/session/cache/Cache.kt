package com.decade.practice.session.cache

interface Cache<I, T> {
      fun save(i: I, t: T)
      fun get(i: I): T?
}