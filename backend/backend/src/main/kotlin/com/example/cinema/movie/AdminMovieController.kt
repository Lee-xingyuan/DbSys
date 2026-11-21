package com.example.cinema.movie

import com.example.cinema.auth.CinemaPrincipal
import com.example.cinema.common.PageResponse
import com.example.cinema.common.PageRequest
import com.example.cinema.common.requireAdminUserId
import com.example.cinema.common.getRequestId
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*


@RestController
class AdminMovieController(private val movieService: AdminMovieService) {
    @GetMapping("/api/admin/movies")
    fun listMovies(
    @AuthenticationPrincipal principal: CinemaPrincipal,
    @RequestParam(defaultValue = "1") page: Int,
    @RequestParam(defaultValue = "20") pageSize: Int,
    ) : PageResponse<MovieRow>{
        requireAdminUserId(principal);
        return movieService.listMovies(PageRequest(page,pageSize))
    }

    @GetMapping("/api/admin/movies/{id}")
    fun getMovie(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name="id") movieId: Long,
    ):MovieRow{
        requireAdminUserId(principal);
        return movieService.getMovieById(movieId)
    }

    @PostMapping("/api/admin/movies")
    fun createMovie(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @Valid @RequestBody movieRequest:SaveMovieRequest,
        httpRequest: HttpServletRequest,
    ) =
        movieService.saveMovie(null,
        movieRequest,
        requireAdminUserId(principal),
        getRequestId(httpRequest))

    @PutMapping("/api/admin/movies/{id}")
    fun updateMovie(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name = "id") movieId: Long,
        @Valid @RequestBody movieRequest:SaveMovieRequest,
        httpRequest: HttpServletRequest,
    )= movieService.saveMovie(movieId,
        movieRequest,
        requireAdminUserId(principal),
        getRequestId(httpRequest))

    @PostMapping("/api/admin/movies/{id}/listing")
    fun changeMovieListingStatus(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name = "id") movieId: Long,
        @RequestBody listingRequest: ListingInput,
        httpRequest: HttpServletRequest
    )= movieService.changeMovieListingStatus(movieId,
        listingRequest.listed,
        requireAdminUserId(principal),
        getRequestId(httpRequest))


}
