package com.example.cinema.auth

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val registerService: RegisterService)  {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): UserProfileResponse =
        registerService.register(request)
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: CinemaPrincipal): UserProfileResponse =
        UserProfileResponse(principal.userId,principal.username,principal.role)

        @GetMapping("/csrf")
        fun csrf(token: CsrfToken) :Map<String, String> = mapOf(
            "headerName" to token.headerName,
            "token" to token.token,
        )



}