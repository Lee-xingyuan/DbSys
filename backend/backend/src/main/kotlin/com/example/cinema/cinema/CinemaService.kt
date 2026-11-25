package com.example.cinema.cinema

import com.example.cinema.audit.AuditService
import com.example.cinema.common.*
import java.time.Clock
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class CinemaService(
    private val cinemaRepository: CinemaRepository,
    private val audit: AuditService,
    private val clock: Clock
) {
    private val cinemaMapper = cinemaRepository.mapper

    @Transactional(readOnly = true)
    fun listCinemas(includeInactive: Boolean, pageRequest: PageRequest) =
        PageResponse(
            items = cinemaMapper.selectCinemaPage(includeInactive, pageRequest),
            page = pageRequest.page,
            pageSize = pageRequest.pageSize,
            total = cinemaMapper.countCinemas(includeInactive)
        )


    @Transactional(readOnly = true)
    fun listHallByCinemaId(cinemaId: Long, pageRequest: PageRequest): PageResponse<HallRow> {
        cinemaMapper.selectCinemaById(cinemaId) ?: notFound("影院不存在")
        return PageResponse(
            items = cinemaMapper.selectHallPageByCinemaId(cinemaId, pageRequest),
            page = pageRequest.page,
            pageSize = pageRequest.pageSize,
            total = cinemaMapper.countHallByCinemaId(cinemaId)
        )
    }

    fun getCinemaById(cinemaId: Long) = cinemaMapper.selectCinemaById(cinemaId) ?: notFound("影院不存在")

    fun getHallById(hallId: Long) = cinemaMapper.selectHallById(hallId) ?: notFound("影厅不存在")

    fun listSeatsByHallId(hallId: Long): List<SeatRow> {
        getHallById(hallId)
        return cinemaMapper.selectSeatsByHallId(hallId)
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun saveCinema(
        cinemaId: Long?,
        cinemaRequest: CinemaInput,
        actorUserId: Long,
        requestId: String
    ): CinemaRow {
        val cinema = if (cinemaId == null) CinemaRow()
        else cinemaMapper.selectCinemaByIdForUpdate(cinemaId) ?: notFound("影院不存在")
        cinema.cinemaName = trimAndValidateText(cinemaRequest.cinemaName, 100)
        cinema.address = trimAndValidateText(cinemaRequest.address, 255)
        cinema.contactPhone = trimAndValidateText(cinemaRequest.contactPhone, 32)

        if (!cinema.contactPhone.matches(Regex("[0-9+() -]{3,32}")))
            badRequest("INVALID_PHONE", "联系电话格式错误")
        if (cinemaId == null) {
            checkSingleRowAffected(cinemaMapper.insertCinema(cinema))
            check(cinema.cinemaId > 0)
        }
        else checkSingleRowAffected(cinemaMapper.updateCinema(cinema))

        audit.recordAuditLog(
            actorUserId,
            "CINEMA_SAVE",
            "CINEMA",
            cinema.cinemaId,
            getCurrentBusinessTime(clock),
            requestId,
            "保存影院资料"
        )
        return checkNotNull(cinemaMapper.selectCinemaById(cinema.cinemaId))
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun saveHall(cinemaId:Long,
                 hallId:Long?,
                 hallRequest: HallInput,
                 actorUserId: Long,
                 requestId: String): HallRow {
        cinemaMapper.selectCinemaByIdForUpdate(cinemaId) ?: notFound("影院不存在")
        val hall =
            if (hallId == null)     HallRow().also { it.cinemaId=cinemaId }
            else    cinemaMapper.selectHallByIdForUpdate(hallId) ?: notFound("影厅不存在")

        if(hall.cinemaId != cinemaId)
            conflict("HALL_CHANGED" ,"影厅归属已变化")
        hall.hallName=trimAndValidateText(hallRequest.hallName, 64)
        try {
            if(hallId==null){
                checkSingleRowAffected(cinemaMapper.insertHall(hall))
                check(hall.hallId > 0)
            }
            else
                checkSingleRowAffected(cinemaMapper.updateHall(hall))
        }
        catch (e: DuplicateKeyException) {
            conflict("HALL_EXISTS","同影院影厅名重复")}
        audit.recordAuditLog(
            actorUserId,
            "HALL_SAVE",
            "HALL",
            hall.hallId,
            getCurrentBusinessTime(clock),
            requestId,
            "保存影厅资料"
        )
        return checkNotNull(cinemaMapper.selectHallById(hall.hallId))
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    fun deactivateCinemaOrHall(
        resourceType: String,
        resourceId: Long,
        actorUserId: Long,
        requestId: String
    ){
        val currentTime=getCurrentBusinessTime(clock)
        if(resourceType=="CINEMA"){
            cinemaMapper.selectCinemaByIdForUpdate(resourceId) ?: notFound("影院不存在")
            if( cinemaMapper.selectFutureScreeningIdByCinemaIdForUpdate(
                    resourceId, currentTime) != null  )
                conflict("FUTURE_SCREENINGS","影院仍有未来场次")
            cinemaMapper.updateCinemaToInactive(resourceId)
        }
        else{
            val existingHall = cinemaMapper.selectHallById(resourceId) ?: notFound("影厅不存在")
            cinemaMapper.selectCinemaByIdForUpdate(existingHall.cinemaId) ?: notFound("影院不存在")
            cinemaMapper.selectHallByIdForUpdate(resourceId) ?: notFound("影厅不存在")

            if(cinemaMapper.selectFutureScreeningIdByHallIdForUpdate(resourceId, currentTime) != null)
                conflict("FUTURE_SCREENINGS","影厅仍有未来场次")
                cinemaMapper.updateHallToInactive(resourceId)
        }
        audit.recordAuditLog(
            actorUserId,
            "${resourceType}_DEACTIVATE",
            resourceType,
            resourceId,
            currentTime,
            requestId,
            "停用资料"
        )
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    fun replaceHallSeatLayout(
        hallId:Long,
        layoutRequest: LayoutInput,
        actorUserId: Long,
        requestId: String
    ):List<SeatRow>{
        val existingHall = getHallById(hallId)
        cinemaMapper.selectCinemaByIdForUpdate(existingHall.cinemaId) ?: notFound("影厅不存在")
        if(layoutRequest.seats.size !in 1..1000
            || layoutRequest.seats.any{it.rowNumber !in 1..32767 || it.seatNumber !in 1..32767}
            || layoutRequest.seats.map{it.rowNumber to it.seatNumber}.distinct().size != layoutRequest.seats.size)
               badRequest("INVALID_LAYOUT","布局需要1~1000个有效选座")
        if(cinemaMapper.selectScreeningByHallIdForUpdate(hallId)!=null)
            conflict("LAYOUT_FROZEN","已有排片，不能重建排片")
        cinemaMapper.deleteSeatsByHallId(hallId)
        layoutRequest.seats.forEach {
            seatDefinition -> checkSingleRowAffected(cinemaMapper.insertSeat(SeatRow().also{
                it.hallId = hallId
            it.rowNumber = seatDefinition.rowNumber
            it.seatNumber=seatDefinition.seatNumber
            it.isActive = seatDefinition.active}
            ))
        }
        audit.recordAuditLog(
            actorUserId,
            "SEAT_LAYOUT",
            "HALL",
            hallId,
            getCurrentBusinessTime(clock),
            requestId,
            "重建座位，共${layoutRequest.seats.size}座"
        )
        return cinemaMapper.selectSeatsByHallId(hallId)
    }

}
