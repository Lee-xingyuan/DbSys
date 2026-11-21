package com.example.cinema.auth

import org.apache.ibatis.annotations.Arg
import org.apache.ibatis.annotations.ConstructorArgs
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import java.time.LocalDateTime

class InsertUserCommand(
    val username: String,
    val passwordHash :String,
    val role: String,
    val registeredAt: LocalDateTime,
    var userId: Long? = null
)


@Mapper
interface AuthMapper {
    @ConstructorArgs(value = [
        Arg(column = "user_id" , javaType = Long::class , id = true),
        Arg(column = "username" , javaType = String::class ),
        Arg(column = "password_hash" , javaType = String::class ),
        Arg(column = "role" , javaType = String::class ),
        Arg(column = "is_active" , javaType = Boolean::class ),
    ])
    @Select("""
        select user_id ,username ,password_hash ,role ,is_active 
        from app_user
        where username = #{username}
    """)
    fun findByUsername(@Param("username") username:String):UserRow?

    @Options(useGeneratedKeys = true, keyProperty = "userId", keyColumn = "user_id")
    @Insert("""
           insert into app_user(username , password_hash, role, is_active,registered_at) 
           values(#{username} , #{passwordHash},#{role},1,#{registeredAt})
    """)
    fun insert(command: InsertUserCommand):Int

}