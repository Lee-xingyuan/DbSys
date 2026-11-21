package com.example.cinema.movie

import com.example.cinema.common.PageRequest
import java.time.LocalDateTime
import org.apache.ibatis.annotations.*

@Mapper
interface AdminMovieMapper {
    @Select("select * from movie where movie_id=#{movieId}")
    fun selectMovieById( movieId:Long ) : MovieRow?

    @Select("select * from movie where movie_id=#{movieId} for update")
    fun selectMovieByIdForUpdate( movieId:Long ) : MovieRow?

    @Insert("""
        insert into movie(title,synopsis,genre,duration_minutes,poster_url,listing_status)
        values(#{title},#{synopsis},#{genre},#{durationMinutes},#{posterUrl,jdbcType=VARCHAR},'DRAFT')
    """)
    @Options(useGeneratedKeys = true, keyProperty = "movieId", keyColumn = "movie_id")
    fun insertMovie(movieRow: MovieRow) :Int

    @Update("""
        update movie
        set title=#{title}, synopsis=#{synopsis}, genre=#{genre},
            duration_minutes=#{durationMinutes}, poster_url=#{posterUrl,jdbcType=VARCHAR}
        where movie_id =#{movieId}
    """)
    fun updateMovie(movieRow: MovieRow) :Int


    @Update("""
        update movie set listing_status = #{listingStatus}
        where movie_id=#{movieId}
    """)
    fun updateMovieListingStatus(@Param("movieId") movieId:Long,
                                 @Param("listingStatus")listingStatus:String
    ):Int

    @Select("""
        select screening_id 
        from screening 
        where movie_id=#{movieId} and screening_status='SCHEDULED' and starts_at > #{currentTime} 
        limit 1 for update
    """)
    fun selectFutureScreeningIdByMovieIdForUpdate(
        @Param("movieId")   movieId:Long,
        @Param("currentTime")currentTime: LocalDateTime
    ):Long?

    @Select("""
        select *
        from movie 
        order by movie_id desc 
        limit #{pageRequest.pageSize} 
        offset #{pageRequest.offset}
    """)
    fun selectMoviePage(@Param("pageRequest") pageRequest:PageRequest): List<MovieRow>

    @Select("select count(*) from movie")
    fun countMovies():Long

}
