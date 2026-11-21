package com.example.cinema.movie

class MovieRow {
    var movieId = 0L; var title = ""; var synopsis = "";
    var genre =""; var durationMinutes =0; var posterUrl :String? = null;
    var listingStatus = ""
}

data class ListingInput(val listed:Boolean)