package com.example.cinema.auth

import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthRepository(private val mapper: AuthMapper) {
    fun findByUsername(username: String): UserRow? = mapper.findByUsername(username)

    fun insert(username :String ,
               passwordHash:String ,
               role:String ,
               registeredAt: LocalDateTime)
    :Long {
        require(role == "CUSTOMER" ||  role == "ADMIN")
        val command = InsertUserCommand(username ,passwordHash,role, registeredAt)
        check(mapper.insert(command) == 1){"账户插入数量异常"}
        return checkNotNull (command.userId ) {"账户缺少生成的编号"}
    }
}