package com.example.cinema.movie

import org.springframework.stereotype.Repository

@Repository
class MovieRepository(val mapper: MovieMapper) {

    fun findListedById(movieId : Long): MovieDetailResponse? =
        mapper.findListedById(movieId)

    fun searchListed(keyword:String? , pageSize:Int, offset:Long) : List<MovieDetailResponse> =
        mapper.searchListed(keyword, pageSize, offset)

    fun countListed(keyword: String?) :Long =
        mapper.countListed(keyword)
}
