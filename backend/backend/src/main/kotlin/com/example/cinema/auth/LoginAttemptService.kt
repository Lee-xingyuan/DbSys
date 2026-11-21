package com.example.cinema.auth

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.Locale


@Service
class LoginAttemptService (
    private val users : AuthRepository,
    private val guards: LoginGuardRepository,
    private val encoder : PasswordEncoder,
    private val clock: Clock,
    ){
    // 每次启动只生成一次；未知用户名仍执行一次密码匹配。
    private val dummyHash: String = checkNotNull(encoder.encode("dummy-password-for-timing-only"))

    @Transactional
    fun attempt(rawUsername: String, rawPassword: String): LoginAttemptResult {
        if(!Regex("[A-Za-z0-9_]{3,32}").matches(rawUsername))
        {
            return LoginAttemptResult.Denied
        }
        val username = rawUsername.lowercase()
        val lockedUntil = guards.lock(username)
        val now = LocalDateTime.now(clock).withNano(0)

        if(lockedUntil != null && now.isBefore(lockedUntil) ) {
            return LoginAttemptResult.Denied
        }

        if(lockedUntil != null )    guards.clear(username)

        val from = now.minusMinutes(10)
        guards.removeBefore(username ,from)

        val user = users.findByUsername(username)
        val validLength = rawPassword.length in 12..64
        val matches = encoder.matches(
            if(validLength) rawPassword else "invalid-password-length"
            ,if(validLength && user != null) user.passwordHash else dummyHash
        )
        if(validLength && matches && user !=null && user.active){
            guards.clear(username)
            return LoginAttemptResult.Success(CinemaPrincipal(user))
        }

        guards.recordFailure(username,now)
        if(guards.countSince(username,from) >= 5 ){
        guards.lockUntil(username,now.plusMinutes(10))
        }
        return LoginAttemptResult.Denied
    }


}