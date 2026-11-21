package com.example.cinema.movie

import com.example.cinema.common.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/movies")
class MovieController(private val service: MovieService) {
    @GetMapping("/{movieId}")
    fun getDetail(@PathVariable("movieId") movieId:Long): MovieDetailResponse=
        service.getDetail(movieId)

    @GetMapping
    fun list(
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "pageSize", defaultValue = "20") pageSize: Int,
    ): PageResponse<MovieSummaryResponse> = service.list(keyword , page, pageSize)
}