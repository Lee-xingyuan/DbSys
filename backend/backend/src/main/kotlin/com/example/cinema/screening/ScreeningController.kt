package com.example.cinema.screening

import com.example.cinema.auth.CinemaPrincipal
import com.example.cinema.common.PageResponse
import com.example.cinema.common.PageRequest
import com.example.cinema.common.requireAdminUserId
import com.example.cinema.common.getRequestId
import jakarta.servlet.http.HttpServletRequest
import java.time.LocalDate
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class ScreeningController(private val screeningService: ScreeningService
    ) {
    @GetMapping("/api/movies/{id}/screenings")
    fun listMovieScreenings(
        @PathVariable(name = "id") movieId: Long,
        @RequestParam(required=false) cinemaId: Long?,
        @RequestParam(name = "date") screeningDate: LocalDate,
        @RequestParam(defaultValue="1") page: Int,
        @RequestParam(defaultValue="20") pageSize: Int
    )=screeningService.listScreenings(movieId,cinemaId,screeningDate,false,PageRequest(page,pageSize))

    @GetMapping("/api/screenings/{id}/seats")
    fun getScreeningSeatMap(
        @PathVariable(name = "id") screeningId: Long,
    )=screeningService.getScreeningSeatMap(screeningId)

    @GetMapping("/api/admin/screenings")
    fun listAdminScreenings(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @RequestParam(required=false) movieId : Long?,
        @RequestParam(required=false) cinemaId: Long?,
        @RequestParam(name = "date") screeningDate: LocalDate,
        @RequestParam(defaultValue="1") page: Int,
        @RequestParam(defaultValue="20") pageSize: Int
    ): PageResponse<ScreeningResponse> {
        requireAdminUserId(principal)
        return screeningService.listScreenings(movieId,cinemaId,screeningDate,true,PageRequest(page,pageSize))
    }

    @GetMapping("/api/admin/screenings/{id}")
    fun getScreeningDetails(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name = "id") screeningId: Long
    ):ScreeningResponse {
        requireAdminUserId(principal)
        return screeningService.getScreeningDetails(screeningId)
    }

    @PostMapping("/api/admin/screenings")
    fun createScreening(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @RequestBody screeningRequest: ScreeningInput,
        httpServletRequest: HttpServletRequest
    )= screeningService.saveScreening(
        null,
        screeningRequest,
        null,
        requireAdminUserId(principal),
        getRequestId(httpServletRequest)
    )

    @PutMapping("/api/admin/screenings/{id}")
    fun updateScreening(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name = "id") screeningId: Long,
        @RequestBody screeningRequest: ScreeningUpdate,
        httpServletRequest: HttpServletRequest
    )=screeningService.saveScreening(
        screeningId,
        ScreeningInput(screeningRequest.movieId,screeningRequest.hallId,screeningRequest.startsAt,screeningRequest.ticketPrice),
        screeningRequest.expectedRevision,
        requireAdminUserId(principal),
        getRequestId(httpServletRequest),
    )

    @PostMapping("/api/admin/screenings/{id}/cancel")
    fun cancelScreening(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name = "id") screeningId: Long,
        @RequestBody cancelRequest: RevisionInput,
        httpServletRequest: HttpServletRequest
    )=screeningService.cancelScreening(
        screeningId,
        cancelRequest.expectedRevision,
        requireAdminUserId(principal),
        getRequestId(httpServletRequest),
    )

}