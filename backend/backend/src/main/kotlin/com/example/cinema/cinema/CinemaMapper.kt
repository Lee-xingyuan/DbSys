package com.example.cinema.cinema

import com.example.cinema.common.PageRequest
import com.example.cinema.common.PageResponse
import java.time.LocalDateTime
import org.apache.ibatis.annotations.*
import org.springframework.beans.propertyeditors.CurrencyEditor
import org.springframework.cglib.core.Local

@Mapper
interface CinemaMapper {

    @Select("select * from cinema where cinema_id=#{cinemaId}")
    fun selectCinemaById(@Param("cinemaId") cinemaId: Long): CinemaRow?

    @Select("select * from cinema where cinema_id=#{cinemaId} for update")
    fun selectCinemaByIdForUpdate(@Param("cinemaId") cinemaId: Long): CinemaRow?

    @Insert("""
        insert into cinema (cinema_name,address,contact_phone,is_active)
        values(#{cinemaName},#{address},#{contactPhone},1)
    """)
    @Options(useGeneratedKeys = true,keyProperty = "cinemaId", keyColumn = "cinema_id")
    fun insertCinema(cinemaRow: CinemaRow):Int

    @Select("select * from hall where hall_id=#{hallId}")
    fun selectHallById(@Param("hallId") hallId: Long): HallRow?

    @Select("select * from hall where hall_id=#{hallId} for update")
    fun selectHallByIdForUpdate(@Param("hallId") hallId: Long): HallRow?

    @Insert("""
        insert into hall(cinema_id,hall_name,is_active) 
        values(#{cinemaId},#{hallName},1)
    """)
    @Options(useGeneratedKeys = true, keyProperty = "hallId", keyColumn = "hall_id")
    fun insertHall(hallRow: HallRow):Int

    @Update("""
        update cinema set cinema_name = #{cinemaName} ,address=#{address}, contact_phone=#{contactPhone} 
        where cinema_id=#{cinemaId}
        """)
    fun updateCinema(cinemaRow: CinemaRow):Int

    @Update("update hall set hall_name=#{hallName} where hall_id=#{hallId} ")
    fun updateHall(hallRow: HallRow):Int

    @Update("""
        update cinema set is_active = 0
        where cinema_id=#{cinemaId}
    """)
    fun updateCinemaToInactive(cinemaId: Long):Int

    @Update("""
        update hall set is_active = 0
        where hall_id=#{hallId}
    """)
    fun updateHallToInactive(hallId: Long):Int


    @Select("""
        select * 
        from seat
        where hall_id = #{hallId}
        order by `row_number`,seat_number
    """)
    fun selectSeatsByHallId(hallId:Long) : List<SeatRow>

    @Delete("""
        delete from seat
        where hall_id=#{hallId}
    """)
    fun deleteSeatsByHallId(hallId:Long):Int

    @Insert("""
        insert into seat(hall_id,`row_number`,seat_number,is_active)
        values (#{hallId},#{rowNumber},#{seatNumber},#{isActive})
    """)
    fun insertSeat(seatRow: SeatRow):Int

    @Select("""
        select screening_id 
        from screening
        where hall_id=#{hallId} 
        limit 1 for update
    """)
    fun selectScreeningByHallIdForUpdate(hallId:Long):Long?

    @Select("""
        select screening_id
        from screening
        where hall_id=#{hallId} and screening_status= 'SCHEDULED' and starts_at > #{currentTime} 
          limit 1 for update
    """)
    fun selectFutureScreeningIdByHallIdForUpdate(@Param("hallId")hallId:Long,
                                                 @Param("currentTime") currentTime: LocalDateTime):Long?

    @Select("""
        select s.screening_id
        from screening s
        where s.hall_id in ( select hall_id from hall where cinema_id = #{cinemaId} )
            and s.screening_status='SCHEDULED' and s.starts_at > #{currentTime}
        limit 1 for update
    """)
    fun selectFutureScreeningIdByCinemaIdForUpdate(@Param("cinemaId") cinemaId: Long,
                                                   @Param("currentTime") currentTime: LocalDateTime):Long?

    @Select("""
        select * 
        from cinema 
        where (#{includeInactive} = true or is_active =1 )
         order by cinema_id 
         limit #{pageRequest.pageSize} 
         offset #{pageRequest.offset}
    """)
    fun selectCinemaPage(@Param("includeInactive") includeInactive: Boolean,
                         @Param("pageRequest") pageRequest: PageRequest): List<CinemaRow>

    @Select("""
        select count(*)
        from cinema
        where (#{includeInactive} = true or is_active =1 )
    """)
    fun countCinemas(includeInactive: Boolean): Long

    @Select("""
        select *
        from hall 
        where cinema_id=#{cinemaId}
        order by hall_id
        limit #{pageRequest.pageSize}
        offset #{pageRequest.offset}
    """)
    fun selectHallPageByCinemaId(@Param("cinemaId") cinemaId:Long,
                                 @Param("pageRequest") pageRequest: PageRequest): List<HallRow>

    @Select("""
        select count(*)
        from hall
        where cinema_id=#{cinemaId}
    """)
    fun countHallByCinemaId(@Param("cinemaId") cinemaId:Long):Long

}
