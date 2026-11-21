package com.example.cinema.auth

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Pattern(regexp = "[A-Za-z0-9_]{3,32}")
    val username:String,
    @field:Size(min = 12, max = 64)
    val password:String
)