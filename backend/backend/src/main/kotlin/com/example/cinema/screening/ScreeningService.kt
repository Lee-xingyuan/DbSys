package com.example.cinema.screening

import com.example.cinema.audit.AuditService
import com.example.cinema.cinema.CinemaRepository
import com.example.cinema.common.*
import com.example.cinema.movie.AdminMovieRepository
import java.math.BigDecimal
import java.time.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class ScreeningService(
    private val screeningRepository: ScreeningRepository,
    private val cinemaRepository: CinemaRepository,
    private val movieRepository: AdminMovieRepository,
    private val audit: AuditService,
    private  val clock: Clock,
) {

    private val mapper get()=screeningRepository.mapper

    private fun toScreeningResponse(
        screeningView: ScreeningViewRow,
        currentTime: LocalDateTime,
    ): ScreeningResponse{
        val  status=when{
            screeningView.screeningStatus!="SCHEDULED"  ||
                    !currentTime.isBefore(screeningView.startsAt.minusMinutes(5))->"STOPPED"
            screeningView.available>0 ->"SELLING"
            screeningView.held>0 -> "TEMPORARILY_FULL"

            else-> "SOLD_OUT"
        }
        return ScreeningResponse(
            screeningView.screeningId,
            screeningView.revision,
            screeningView.movieId,
            screeningView.title,
            screeningView.cinemaId,
            screeningView.cinemaName,
            screeningView.address,
            screeningView.hallId,
            screeningView.hallName,
            screeningView.startsAt.asApiTime(),
            screeningView.endsAt.asApiTime(),
            screeningView.ticketPrice.asMoney(),
            status,
            screeningView.screeningStatus
        )
    }

    @Transactional(readOnly = true)
    fun listScreenings(
        movieId:Long?,
        cinemaId:Long?,
        screeningDate:LocalDate,
        includeCancelled:Boolean,
        pageRequest: PageRequest
    ):PageResponse<ScreeningResponse>{
        val currentTime = getCurrentBusinessTime(clock)
        val startsAtFromInclusive = screeningDate.atStartOfDay()
        val startsAtToExclusive = screeningDate.plusDays(1).atStartOfDay()

        return PageResponse(
            items = mapper.selectScreeningPage(movieId,
                cinemaId,
                startsAtFromInclusive ,
                startsAtToExclusive ,
                    includeCancelled ,
                pageRequest
            ).map{toScreeningResponse(it,currentTime)},

            pageRequest.page,
            pageRequest.pageSize,
            mapper.countScreenings(
                movieId,
                cinemaId,
                startsAtFromInclusive,
                startsAtToExclusive,
                includeCancelled
            )
        )
    }

    fun getScreeningDetails(screeningId:Long)
            =toScreeningResponse(
                mapper.selectScreeningViewById(screeningId) ?: notFound("场次不存在"),
                getCurrentBusinessTime(clock)
            )


    @Transactional(readOnly = true)
    fun getScreeningSeatMap(screeningId:Long): SeatMapResponse{
        val screening=mapper.selectScreeningById(screeningId) ?: notFound("场次不存在")
        if(screening.screeningStatus!="SCHEDULED")
            conflict("SCREENING_CANCELLED","场次已取消")
        return SeatMapResponse(screeningId,
            screening.revision,
            getCurrentBusinessTime(clock).asApiTime(),
            screening.startsAt.minusMinutes(5).asApiTime(),
            screening.ticketPrice.asMoney(),
            mapper.selectScreeningSeatsByScreeningId(screeningId))
    }

    private fun lockParentResources(
        originalScreening:ScreeningRow?,
        movieId:Long,
        hallId:Long
    ){
        val hallIds =(listOfNotNull(originalScreening?.hallId) + hallId).distinct().sorted()
        val originalHalls = hallIds.associateWith {
            cinemaRepository.mapper.selectHallById(it) ?: notFound("影厅不存在") }
        originalHalls.values.map{it.cinemaId}.distinct().sorted().forEach {
            cinemaRepository.mapper.selectCinemaByIdForUpdate(it) ?: notFound("影院不存在") }
        (listOfNotNull(originalScreening?.movieId) + movieId).distinct().sorted().forEach {
            movieRepository.mapper.selectMovieByIdForUpdate(it) ?: notFound("影片不存在")
        }
        hallIds.forEach { hallId ->
            val lockHall =cinemaRepository.mapper.selectHallByIdForUpdate(hallId) ?: notFound("影厅不存在")
            if(lockHall.cinemaId != originalHalls.getValue(hallId).cinemaId )
                conflict("PARENT_CHANGED","父资料变化，请重试")
        }
    }

    private fun checkScreeningEditable(
        screening:ScreeningRow,
        expectedRevision:Int
    ){
        if(screening.revision!=expectedRevision)
            conflict("SCREENING_CHANGED","修订号过期，请刷新")
        if(screening.screeningStatus!="SCHEDULED" ||
            !screening.startsAt.isAfter(getCurrentBusinessTime(clock)))
            conflict("SCREENING_FROZEN","场次不能修改")
        if(mapper.selectOrderIdByScreeningIdForUpdate(screening.screeningId)!=null)
            conflict("SCREENING_FROZEN","有订单的场次不能修改或取消")
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    fun saveScreening(
        screeningId:Long?,
        screeningRequest: ScreeningInput,
        expectedRevision: Int?,
        actorUserId: Long,
        requestId:String
    ): ScreeningResponse{
        if( screeningRequest.movieId<=0 ||
            screeningRequest.hallId<=0 ||
            screeningRequest.startsAt.nano!=0 ||
            screeningRequest.ticketPrice.scale()>2 ||
            screeningRequest.ticketPrice<BigDecimal("0.01") ||
            screeningRequest.ticketPrice>BigDecimal("9999.99"))
                badRequest("INVALID_SCREENING","编号、时间精度或者票价无效")
        val originalScreening =
            if(screeningId == null) null
            else mapper.selectScreeningById(screeningId)  ?: notFound("场次不存在")

        lockParentResources(originalScreening,screeningRequest.movieId,screeningRequest.hallId)

        val screening =
                if(screeningId==null) ScreeningRow()
                else mapper.selectScreeningByIdForUpdate(screeningId) ?: notFound("场次不存在")

        if(originalScreening!=null &&
            (screening.hallId!=originalScreening.hallId || screening.movieId!=originalScreening.movieId)
            )
            conflict("PARENT_CHANGED","场次归属变化，请刷新重试")
        if(screeningId!=null)
            checkScreeningEditable(screening,expectedRevision ?: 0)

        val movie=checkNotNull(movieRepository.mapper.selectMovieById(screeningRequest.movieId))
        val hall=checkNotNull(cinemaRepository.mapper.selectHallById(screeningRequest.hallId))
        val cinema=checkNotNull(cinemaRepository.mapper.selectCinemaById(hall.cinemaId))

        if(movie.listingStatus!="LISTED" || !hall.isActive || !cinema.isActive)
            conflict("INACTIVE_RESOURCE","资料未启用或影片未上架")
        val physicalSeats=cinemaRepository.mapper.selectSeatsByHallId(hall.hallId)
        if(physicalSeats.none { it.isActive }) conflict("EMPTY_HALL","影厅没有可售座位")

        screening.movieId=screeningRequest.movieId; screening.hallId=screeningRequest.hallId;
        screening.startsAt=screeningRequest.startsAt.atZoneSameInstant(clock.zone).toLocalDateTime()

        if(!screening.startsAt.minusMinutes(5).isAfter(getCurrentBusinessTime(clock)))
            badRequest("INVALID_START","开始时间必须晚于当前时间5分钟")

        screening.endsAt=screening.startsAt.plusMinutes(movie.durationMinutes.toLong())
        screening.ticketPrice=screeningRequest.ticketPrice

        if(mapper.selectConflictingScreeningIdsForUpdate(
            screening.hallId,
            screeningId ?: 0,
            screening.startsAt,
            screening.endsAt.plusMinutes(15)
            ).isNotEmpty()
        )   conflict("SCREENING_OVERLAP","场次含15分钟清场时间发生冲突")

        if(screeningId==null) {
            checkSingleRowAffected(mapper.insertScreening(screening))
            check(screening.screeningId > 0)
        }
        else {
            checkSingleRowAffected(mapper.updateScreeningByIdAndRevision(screening, expectedRevision!!))
            mapper.deleteScreeningSeatsByScreeningId(screeningId)
        }
        check(mapper.insertScreeningSeatsFromHall(screening.screeningId,screening.hallId)==physicalSeats.size)

        audit.recordAuditLog(
            actorUserId,
            if(screeningId==null) "SCREENING_CREATE" else "SCREENING_UPDATE",
            "SCREENING",
            screening.screeningId,
             getCurrentBusinessTime(clock),
             requestId,
             "保存排片并生成库存"
        )
        return getScreeningDetails(screening.screeningId)
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    fun cancelScreening(screeningId: Long,
                        expectedRevision: Int,
                        actorUserId: Long,
                        requestId: String):
            ScreeningResponse {
            val originalScreening=mapper.selectScreeningById(screeningId) ?: notFound("场次不存在");
            lockParentResources(originalScreening,originalScreening.movieId,originalScreening.hallId)

            val screening=mapper.selectScreeningByIdForUpdate(screeningId) ?: notFound("场次不存在")

            if(screening.hallId!=originalScreening.hallId ||
                screening.movieId!=originalScreening.movieId)
                conflict("PARENT_CHANGED","归属变化，请刷新重试")

            checkScreeningEditable(screening,expectedRevision)

            checkSingleRowAffected(mapper.updateScreeningToCancelledByIdAndRevision(screeningId,expectedRevision))

            audit.recordAuditLog(
                actorUserId = actorUserId,
                actionType = "SCREENING_CANCEL",
                targetType = "SCREENING",
                targetId = screeningId,
                occurredAt = getCurrentBusinessTime(clock),
                requestId = requestId,
                changeSummary = "取消未产生订单的排片"
            )
            return getScreeningDetails(screeningId)
    }


}