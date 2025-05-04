package com.decade.practice.utils


infix fun <T> List<T>.union(other: List<T>): List<T> {
      return plus(other.filter { !contains(it) })
}
