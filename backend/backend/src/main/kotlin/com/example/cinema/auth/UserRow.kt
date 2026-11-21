package com.example.cinema.auth

class UserRow(val userId:Long,
              val username:String,
              val passwordHash:String,
              val role : String,
              val active : Boolean
)