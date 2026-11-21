package com.example.cinema.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime
import java.util.Locale

@Component
@Profile("init-admin")
class AdminAccountInitializer(private val users : AuthRepository,
                                    private val encode: PasswordEncoder,
                                    private val clock: Clock,
                                    @param:Value("\${ADMIN_USERNAME}") private val username: String,
                                    @param:Value("\${ADMIN_PASSWORD}") private val password: String) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        require(Regex("[A-Za-z0-9_]{3,32}").matches(username)){
            "管理员用户名不符合规则"
        }
        require(password.length in 12..64) {
            "管理员长度不符合规范"}
        val normalized =username.lowercase()
        check(users.findByUsername(normalized) == null){
            "该用户名已存在"
        }
        users.insert(normalized,
            checkNotNull(encode.encode(password)),
            "ADMIN",
            LocalDateTime.now(clock).withNano(0)
            )
    }

}