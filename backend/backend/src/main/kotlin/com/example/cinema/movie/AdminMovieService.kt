package com.example.cinema.movie

import com.example.cinema.audit.AuditService
import com.example.cinema.common.*
import java.net.URI
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class AdminMovieService(
    private val movieRepository: AdminMovieRepository,
    private val audit: AuditService,
    private val clock: Clock,
    private val requestIdFilter: RequestIdFilter,
){

    private val movieMapper get() =movieRepository.mapper

    @Transactional(readOnly = true)
    fun listMovies(pageRequest: PageRequest) = PageResponse(
            items = movieMapper.selectMoviePage(pageRequest),
            page= pageRequest.page,
            pageSize = pageRequest.pageSize,
            total = movieMapper.countMovies()
            )

    fun getMovieById(movieId:Long) =
        movieMapper.selectMovieById(movieId) ?: notFound("影片不存在")

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun saveMovie(
        movieId:Long?,
        movieRequest: SaveMovieRequest,
        actorUserId:Long,
        requestId:String
    ):MovieRow {
        val movie = if(movieId==null) MovieRow()
                    else movieMapper.selectMovieByIdForUpdate(movieId) ?: notFound("影片不存在")
        movie.title = trimAndValidateText(movieRequest.title,120)
        movie.synopsis = trimAndValidateText(movieRequest.synopsis,10000)
        movie.genre = trimAndValidateText(movieRequest.genre,40)

        if(movieRequest.durationMinutes !in 1..32767 || movie.synopsis.toByteArray(Charsets.UTF_8).size > 65535)
            badRequest("INVALIDATE_MOVIE","片长或者简介无效")
        val poster = movieRequest.posterUrl?.trim()?.takeIf{ it.isNotEmpty() }
        if(poster!=null) {
            val u=try {
                URI(poster)
            }
            catch (e: Exception) {
                badRequest("INVALID_POSTER","海报地址无效")
            }

            if(poster.length > 500 ||
                !(poster.startsWith("/images/") && !poster.startsWith("//")
                        || u.scheme in listOf("http", "https") && !u.host.isNullOrBlank() ) )
                badRequest("INVALID_POSTER","海报地址无效")
        }

        movie.durationMinutes = movieRequest.durationMinutes
        movie.posterUrl = poster
        if(movieId==null){
            checkSingleRowAffected(movieMapper.insertMovie(movie))
            check(movie.movieId>0)
        }
        else
            movieMapper.updateMovie(movie)

        audit.recordAuditLog(
            actorUserId,
            "MOVIE_SAVE",
            "MOVIE",
            movie.movieId,
            getCurrentBusinessTime(clock),
            requestId,
            "保存影片资料"
        )
        return checkNotNull(movieMapper.selectMovieById(movie.movieId))
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    fun changeMovieListingStatus(
        movieId:Long,
        listed:Boolean,
        actorUserId:Long,
        requestId:String
    ):MovieRow{
        val movie =movieMapper.selectMovieByIdForUpdate(movieId) ?: notFound("影片不存在")
        if(!listed && movieMapper.selectFutureScreeningIdByMovieIdForUpdate(movieId,getCurrentBusinessTime(clock)) != null)
            conflict("FUTURE_SCREENING","影片仍有未来场次")

        if(listed){
            trimAndValidateText(movie.title,120)
            trimAndValidateText(movie.synopsis,10000)
            trimAndValidateText(movie.genre,40)
            check(movie.durationMinutes > 0)
        }
        movieMapper.updateMovieListingStatus(movieId,if(listed) "LISTED" else "UNLISTED")
        audit.recordAuditLog(
            actorUserId,
            "MOVIE_LISTING",
            "MOVIE",
            movie.movieId,
            getCurrentBusinessTime(clock),
            requestId,
            if(listed) "上架影片" else "下架影片"
        )
        return checkNotNull(movieMapper.selectMovieByIdForUpdate(movieId))
    }


}
