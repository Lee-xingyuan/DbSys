package com.example.cinema.movie

class MovieSummaryResponse (
        val movieId :Long,
        val title: String,
        val genre :String,
        val durationMinutes :Int,
        val posterUrl :String?
    )