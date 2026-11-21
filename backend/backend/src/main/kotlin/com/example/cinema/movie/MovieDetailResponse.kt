package com.example.cinema.movie

data class MovieDetailResponse(
    val movieId: Long,
    val title: String,
    val synopsis: String,
    val genre: String,
    val durationMinutes: Int,
    val posterUrl: String?
)
