package com.decade.practice.model.dto

data class SignUpRequest(
      val username: String,
      val password: String,
      val name: String,
      val gender: String,
      val dob: String,
)