package com.decade.practice.session.cache

class SimpleCache<I, T> : Cache<I, T> {
      private val map = HashMap<I, T>()
      override fun save(i: I, t: T) {
            map.put(i, t)
      }

      override fun get(i: I): T? = map.get(i)

}