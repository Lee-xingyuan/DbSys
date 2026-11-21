package com.example.cinema.auth

import org.apache.ibatis.annotations.Arg
import org.apache.ibatis.annotations.ConstructorArgs
import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update
import java.time.LocalDateTime

class LoginGuardRow(val username:String,
                    val lockUntil: LocalDateTime?)

@Mapper
interface LoginGuardMapper {

    @Insert("""
        insert ignore into login_guard (username , locked_until) 
        values (#{username},Null)
    """)
    fun ensureGuard(@Param("username") username:String) :Int

    @ConstructorArgs(value = [
        Arg(column = "username", javaType = String::class,id=true),
        Arg(column= "locked_until",javaType = LocalDateTime::class)
    ])
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select ("""
        select username ,locked_until 
        from login_guard
        where username = #{username}
        for update
    """)
    fun lockGuard(@Param("username") username:String) : LoginGuardRow?

    @Delete("delete from login_failure where username = #{username} ")
    fun deleteFailures(@Param("username") username:String) :Int

    @Update("update login_guard set locked_until= NULL where username = #{username} ")
    fun clearLock(@Param("username") username:String) :Int

    @Delete("delete  from login_failure where username = #{username} and occurred_at < #{from}")
    fun removeBefore(@Param("username") username:String, @Param("from") from: LocalDateTime) :Int

    @Insert("insert into login_failure(username,occurred_at) values (#{username},#{now})  ")
    fun recordFailure(@Param("username") username:String, @Param("now") from: LocalDateTime) :Int

    @Select("""
        select count(*) from login_failure
        where username =#{username} and occurred_at >= #{from}
    """)
    fun countSince(@Param("username") username:String, @Param("from") from: LocalDateTime) :Long

    @Update("update login_guard set locked_until= #{until} where username = #{username} ")
    fun lockUntil(@Param("username") username:String, @Param("until") until: LocalDateTime) :Int


}