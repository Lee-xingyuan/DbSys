package com.example.cinema.auth

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class CinemaAuthenticationProvider(private val attempts: LoginAttemptService): AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val result = attempts.attempt(authentication.name ,
             authentication.credentials as? String ?: ""
        )
        return when (result){
            is LoginAttemptResult.Success -> UsernamePasswordAuthenticationToken(result.principal,
                null,result.principal.authorities)

            LoginAttemptResult.Denied  -> throw BadCredentialsException("用户名或者密码有误，请重试"
            )
        }
    }

    override fun supports(authentication: Class<*>): Boolean =
        UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)

}