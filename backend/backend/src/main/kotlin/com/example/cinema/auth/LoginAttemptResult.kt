package com.example.cinema.auth


sealed interface LoginAttemptResult {
    data class Success(val principal: CinemaPrincipal) : LoginAttemptResult
    data object Denied : LoginAttemptResult

}