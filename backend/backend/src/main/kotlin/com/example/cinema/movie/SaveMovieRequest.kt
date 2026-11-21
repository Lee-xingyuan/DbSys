package com.example.cinema.movie

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class SaveMovieRequest (
    @field:NotBlank @field:Size(max = 120) val title :String,
    @field:NotBlank @field:Size(max = 10000) val synopsis: String,
    @field:NotBlank @field:Size(max = 40) val genre : String,
    @field :Positive @field:Max(32767)   val durationMinutes: Int,
    @field:Size(max=500) val posterUrl : String?
)
