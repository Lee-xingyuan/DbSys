package com.example.cinema.screening

import com.example.cinema.common.*
import java.time.*
import org.apache.ibatis.annotations.*


@Mapper
interface ScreeningMapper {

    @Select("select * from screening where screening_id=#{screeningId}")
    fun selectScreeningById(@Param("screeningId") screeningId: Long):ScreeningRow?

    @Select("""
        select * 
        from screening 
        where screening_id=#{screeningId} 
        for update
        """)
    fun selectScreeningByIdForUpdate(@Param("screeningId") screeningId: Long): ScreeningRow?

    @Select("""
        select screening_id
        from screening
        where hall_id=#{hallId} and screening_status='SCHEDULED' 
            and screening_id <>#{excludeScreeningId} 
            and starts_at < #{newEndsAtWithCleanUp}  and DATE_ADD(ends_at,interval 15 minute)  > #{newStartsAt}
        order by screening_id 
        for update
    """)
    fun selectConflictingScreeningIdsForUpdate
                (@Param("hallId") hallId: Long,
                 @Param("excludeScreeningId") excludeScreeningId: Long,
                 @Param("newStartsAt") newStartsAt: LocalDateTime,
                 @Param("newEndsAtWithCleanUp") newEndsAtWithCleanUp: LocalDateTime
                 ): List<Long>

    @Select("""
        select order_id
        from ticket_order
        where screening_id=#{screeningId}
        limit 1 
        for update
    """)
    fun selectOrderIdByScreeningIdForUpdate(
        @Param("screeningId") screeningId: Long
    ):Long?

    @Insert("""
        insert into screening(movie_id,hall_id,starts_at,ends_at,ticket_price,screening_status,revision)
        values(#{movieId} ,#{hallId},#{startsAt},#{endsAt},#{ticketPrice},'SCHEDULED',1)
    """)
    @Options(useGeneratedKeys = true,keyProperty = "screeningId", keyColumn = "screening_id")
    fun insertScreening(
        screeningRow: ScreeningRow
    ):Int

    @Update("""
        update screening 
        set movie_id= #{screeningRow.movieId} ,hall_id = #{screeningRow.hallId},
        starts_at = #{screeningRow.startsAt} ,ends_at = #{screeningRow.endsAt},
        ticket_price = #{screeningRow.ticketPrice} ,revision=revision+1
        where screening_id=#{screeningRow.screeningId} and revision=#{expectedRevision}
         
    """)
    fun updateScreeningByIdAndRevision(
        @Param("screeningRow") screeningRow: ScreeningRow,
        @Param("expectedRevision") expectedRevision: Int
        ):Int

    @Update("""
        update screening
        set screening_status ='CANCELLED' , revision = revision+1
        where screening_id=#{screeningId} and revision=#{expectedRevision}
    """)
    fun updateScreeningToCancelledByIdAndRevision(
        @Param("screeningId") screeningId: Long,
        @Param("expectedRevision") expectedRevision: Int
    ):Int

    @Delete("""
        delete from screening_seat
        where screening_id=#{screeningId}
    """)
    fun deleteScreeningSeatsByScreeningId(
        screeningId: Long
    ):Int

    @Insert("""
        insert into screening_seat(screening_id , seat_id,inventory_status,current_order_id)
         select #{screeningId} ,seat_id, if(is_active,'AVAILABLE','DISABLED'),null
            from seat
            where hall_id=#{hallId}
    """)
    fun insertScreeningSeatsFromHall(
        @Param("screeningId") screeningId: Long,
        @Param("hallId") hallId: Long
    ):Int

    @Select("""
        select ss.screening_seat_id ,s.`row_number` ,s.seat_number, ss.inventory_status
        from screening_seat ss join seat s 
        on s.seat_id = ss.seat_id
        where ss.screening_id=#{screeningId} 
        order by s.`row_number` , s.seat_number 
        
    """)
    fun selectScreeningSeatsByScreeningId(screeningId:Long) : List<InventorRow>

    //by xml
    fun selectScreeningPage(
        @Param("movieId") movieId: Long?,
        @Param("cinemaId") cinemaId: Long?,
        @Param("startsAtFromInclusive") startsAtFromInclusive: LocalDateTime,
        @Param("startsAtToExclusive")  startsAtToExclusive: LocalDateTime,
        @Param("includeCancelled") includeCancelled:Boolean,
        @Param("pageRequest") pageRequest: PageRequest
    ):List<ScreeningViewRow>

    fun countScreenings(
        @Param("movieId") movieId: Long?,
        @Param("cinemaId") cinemaId: Long?,
        @Param("startsAtFromInclusive") startsAtFromInclusive: LocalDateTime,
        @Param("startsAtToExclusive") startsAtToExclusive: LocalDateTime,
        @Param("includeCancelled") includeCancelled:Boolean,
    ):Long

    fun selectScreeningViewById(
        screeningId: Long
    ):ScreeningViewRow?

}