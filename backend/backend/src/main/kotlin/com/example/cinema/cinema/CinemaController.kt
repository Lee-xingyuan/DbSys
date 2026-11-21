package com.example.cinema.cinema

import com.example.cinema.auth.CinemaPrincipal
import com.example.cinema.common.PageResponse
import com.example.cinema.common.PageRequest
import com.example.cinema.common.requireAdminUserId
import com.example.cinema.common.getRequestId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class CinemaController(private val cinemaService: CinemaService) {

    @GetMapping("/api/cinemas")
    fun listActiveCinemas(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
    )=cinemaService.listCinemas(
        false,
        PageRequest(page, pageSize)
    )

    @GetMapping("/api/admin/cinemas")
    fun listCinemas(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
    ):PageResponse<CinemaRow>{
        requireAdminUserId(principal);
        return cinemaService.listCinemas(true, PageRequest(page, pageSize))
    }

    @GetMapping("/api/admin/cinemas/{id}")
    fun getCinema(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name="id") cinemaId: Long
    ):CinemaRow{
        requireAdminUserId(principal);
        return cinemaService.getCinemaById(cinemaId)
    }

    @PostMapping("/api/admin/cinemas")
    fun createCinema(
     @AuthenticationPrincipal principal: CinemaPrincipal,
     @RequestBody cinemaRequest: CinemaInput,
     httpRequest: HttpServletRequest,
     )= cinemaService.saveCinema(
         null,
         cinemaRequest,
         requireAdminUserId(principal),
         getRequestId(httpRequest)
     )

    @PutMapping("/api/admin/cinemas/{id}")
    fun updateCinema(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name="id") cinemaId:Long,
        @RequestBody cinemaRequest: CinemaInput,
        httpRequest: HttpServletRequest,
    )= cinemaService.saveCinema(cinemaId,
        cinemaRequest,
        requireAdminUserId(principal),
        getRequestId(httpRequest)
    )

    @PostMapping("/api/admin/cinemas/{id}/deactivate")
    fun deactivateCinema(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name="id") cinemaId:Long,
        httpRequest: HttpServletRequest,
    )= cinemaService.deactivateCinemaOrHall(
        "CINEMA",
        cinemaId,
        requireAdminUserId(principal),
        getRequestId(httpRequest)
    )

    @GetMapping("/api/admin/cinemas/{cinemaId}/halls")
    fun listHalls(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable cinemaId: Long,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): PageResponse<HallRow> {
        requireAdminUserId(principal)
        return cinemaService.listHallByCinemaId(cinemaId, PageRequest(page, pageSize))
    }

    @PostMapping("/api/admin/cinemas/{cinemaId}/halls")
    fun createHall(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable cinemaId: Long,
        @RequestBody hallRequest: HallInput,
        httpRequest: HttpServletRequest
    )= cinemaService.saveHall(
        cinemaId,
        null,
        hallRequest,
        requireAdminUserId(principal),
        getRequestId(httpRequest)
    )

    @PutMapping("/api/admin/halls/{id}")
    fun updateHall(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name="id") hallId:Long,
        @RequestBody hallRequest: HallInput,
        httpRequest: HttpServletRequest
    )=cinemaService.saveHall(
        cinemaService.getHallById(hallId).cinemaId,
        hallId,
        hallRequest,
        requireAdminUserId(principal),
        getRequestId(httpRequest)
    )

    @GetMapping("/api/admin/halls/{id}")
    fun getHall(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name="id") hallId:Long,
    ): HallRow{
        requireAdminUserId(principal);
        return cinemaService.getHallById(hallId)
    }
    @GetMapping("/api/admin/halls/{id}/seats")
    fun listHallSeats(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name="id") hallId:Long,
    ):List<SeatRow>{
        requireAdminUserId(principal);
        return  cinemaService.listSeatsByHallId(hallId)
    }

    @PostMapping("/api/admin/halls/{id}/seats")
    fun replaceHallSeatLayout(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name="id") hallId:Long,
        @RequestBody layoutRequest: LayoutInput,
        httpRequest: HttpServletRequest
    )=cinemaService.replaceHallSeatLayout(
        hallId,
        layoutRequest,
        requireAdminUserId(principal),
        getRequestId(httpRequest)
    )

    @PostMapping("/api/admin/halls/{id}/deactivate")
    fun deactivateHall(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name="id") hallId:Long,
        httpRequest: HttpServletRequest
    )=cinemaService.deactivateCinemaOrHall(
        "HALL",
        hallId,
        requireAdminUserId(principal),
        getRequestId(httpRequest)
    )



}
