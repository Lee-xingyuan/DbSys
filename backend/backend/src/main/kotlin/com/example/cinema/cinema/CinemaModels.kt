package com.example.cinema.cinema

import java.awt.ActiveEvent

class CinemaRow{
    var cinemaId=0L; var cinemaName="";
    var address=""; var contactPhone="";var isActive=false
}

class HallRow{
    var hallId=0L; var cinemaId=0L;
    var hallName=""; var isActive=false
}

class SeatRow{
    var seatId=0L; var hallId=0L; var rowNumber =0;
    var seatNumber=0; var isActive=false
}

data class CinemaInput(
    val cinemaName:String,
    val address:String,
    val contactPhone:String)

data class HallInput(val hallName:String)

data class SeatInput(val rowNumber:Int,
                     val seatNumber:Int,
                     val active:Boolean=true)

data class LayoutInput(val seats: List<SeatInput>)
