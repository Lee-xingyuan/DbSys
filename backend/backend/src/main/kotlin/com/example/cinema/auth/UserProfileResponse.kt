package com.example.cinema.auth

data  class UserProfileResponse(
    val userId:Long,
    val username :String,
    val role: String
)