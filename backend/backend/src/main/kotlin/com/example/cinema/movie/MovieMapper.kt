package com.example.cinema.movie

import org.apache.ibatis.annotations.Arg
import org.apache.ibatis.annotations.ConstructorArgs
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface MovieMapper {

    @ConstructorArgs(value = [
        Arg(column = "movie_id", javaType = Long::class,id=true),
        Arg(column = "title", javaType = String::class),
        Arg(column = "synopsis", javaType = String::class),
        Arg(column = "genre", javaType = String::class),
        Arg(column = "duration_minutes", javaType = Int::class),
        Arg(column = "poster_url", javaType = String::class),
    ])
    @Select("""
        select movie_id,title,synopsis,genre,duration_minutes,poster_url
        from movie
        where movie_id = #{movieId} and listing_status = 'Listed'
    """)
    fun findListedById(@Param("movieId") movieId: Long) : MovieDetailResponse?

    fun searchListed(@Param("keyword") keyword:String? ,
                     @Param("pageSize") pageSize : Int,
                     @Param("offset") offset : Long) : List<MovieDetailResponse>

    fun countListed(@Param("keyword") keyword:String? ) : Long

}