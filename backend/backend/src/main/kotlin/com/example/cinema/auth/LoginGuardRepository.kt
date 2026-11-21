package com.example.cinema.auth

import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class LoginGuardRepository(private val mapper: LoginGuardMapper) {
    fun lock(username:String) :LocalDateTime?
    {
        mapper.ensureGuard(username)
        return checkNotNull(mapper.lockGuard(username)){
            "登录保护行不存在"
        }.lockUntil
    }

    fun clear(username :String)
    {
        mapper.deleteFailures(username)
        mapper.clearLock(username)
    }

    fun removeBefore(username: String,from :LocalDateTime){
        mapper.removeBefore(username, from)
    }

    fun recordFailure(username: String,now :LocalDateTime){
        check(mapper.recordFailure(username,now) == 1)
    }

    fun countSince(username :String,from : LocalDateTime) :Long =
        mapper.countSince(username,from)


    fun lockUntil(username : String,until: LocalDateTime){
        mapper.lockUntil(username,until)
    }

}
