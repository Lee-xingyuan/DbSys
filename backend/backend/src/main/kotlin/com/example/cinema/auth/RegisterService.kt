package com.example.cinema.auth

import com.example.cinema.common.badRequest
import com.example.cinema.common.conflict
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class RegisterService(private val repository: AuthRepository, private val encoder: PasswordEncoder, private val clock:Clock) {
    @Transactional
    fun register(req: RegisterRequest): UserProfileResponse{
        if(!Regex("[A-Za-z0-9_]{3,32}").matches(req.username)
            || req.password.length !in 12..64)
        {
            badRequest("INVALID_REGISTER","用户名或者密码不符合规则")
        }
        val username = req.username.lowercase()
        val userId = try{
            repository.insert(username,
                checkNotNull(encoder.encode(req.password)),
                "CUSTOMER",
                LocalDateTime.now(clock).withNano(0))
        } catch (e: DuplicateKeyException) {
            conflict("USERNAME_TAKEN","该用户名被占用")
        }
        return UserProfileResponse(userId,username,"CUSTOMER")
    }


}