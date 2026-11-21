package com.example.cinema.screening

import com.example.cinema.common.*
import java.math.BigDecimal
import java.time.*



class ScreeningRow{
    var screeningId=0L; var movieId=0L; var hallId=0L;
    var startsAt=LocalDateTime.MIN ; var endsAt=LocalDateTime.MIN
    var ticketPrice=BigDecimal.ZERO;
    var screeningStatus="";
    var revision=0
}

class ScreeningViewRow{
    var screeningId=0L; var revision=0; var movieId=0L; var title="";
    var cinemaId=0L;    var hallId=0L
    var cinemaName="";  var address= "";var hallName=""
    var startsAt=LocalDateTime.MIN ; var endsAt=LocalDateTime.MIN
    var ticketPrice=BigDecimal.ZERO;
    var screeningStatus="";
    var available=0L; var held=0L; var sold=0L;

}

class InventorRow{
    var screeningSeatId=0L; var rowNumber=0; var seatNumber=0;
    var inventoryStatus=""
}

data class ScreeningInput(
    val movieId:Long,
    val hallId:Long,
    val startsAt:OffsetDateTime,
    val ticketPrice:BigDecimal,
)

data class ScreeningUpdate(
    val movieId:Long,
    val hallId:Long,
    val startsAt:OffsetDateTime,
    val ticketPrice:BigDecimal,
    val expectedRevison:Int
)

data class RevisionInput(val expectedRevision: Int)

data class ScreeningResponse(
    val screeningId:Long,
    val revision:Int,
    val movieId:Long,
    val movieTitle:String,
    val cinemaId:Long,
    val cinemaName:String,
    val cinemaAddress:String,
    val hallId:Long,
    val hallName:String,
    val startsAt:OffsetDateTime,
    val endsAt:OffsetDateTime,
    val ticketPrice: String,
    val saleStatus:String,
    val status:String
)

data class SeatMapResponse(
    val screeningId:Long,
    val revision:Int,
    val serverTime:OffsetDateTime,
    val salesStopAt:OffsetDateTime,
    val ticketPrice:String,
    val seats:List<InventorRow>
)