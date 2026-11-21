package com.example.cinema.movie

import com.example.cinema.common.PageRequest
import com.example.cinema.common.PageResponse
import com.example.cinema.common.badRequest
import com.example.cinema.common.notFound
import org.springframework.stereotype.Service

@Service
class MovieService(private val repository: MovieRepository) {
    fun getDetail(movieId :Long) :MovieDetailResponse {
        if(movieId <= 0)    badRequest("INVALID_MOVIE_ID","影片编号必须为正数")
        return repository.findListedById(movieId) ?: notFound("影片未找到或未上架")
    }

    fun list(keyword:String? ,page:Int ,pageSize:Int) : PageResponse<MovieSummaryResponse>{
        val pagination = PageRequest(page, pageSize)
        if(keyword != null && keyword.length > 120)
        {
            badRequest("INVALID_KEYWORD","片名或者关键词不能超过120个字符")
        }
        val normalized = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val pattern = normalized?.let{"%$it%"}

        val items = repository.searchListed(pattern ,pagination.pageSize,pagination.offset).map{
            MovieSummaryResponse(it.movieId,it.title,it.genre,it.durationMinutes,it.posterUrl)
        }
        return PageResponse(items , pagination.page ,pagination.pageSize,repository.countListed(pattern))
    }

}
