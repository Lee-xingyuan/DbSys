package com.example.cinema.review

import com.example.cinema.common.*
import java.math.BigDecimal
import java.time.*

class ReviewRow{
    var reviewId=0L; var userId=0L; var movieId=0L;
    var qualifyingOrderId=0L
    var rating=0; var body=""
    var publishedAt=LocalDateTime.MIN ; var updatedAt=LocalDateTime.MIN
    var visibilityStatus="";  var authorName=""
}

class RatingRow{
    var averageRating:BigDecimal? = null
    var reviewCount=0L
}

data class ReviewInput(val rating: Int,val body: String)

data class VisibilityInput(val visible: Boolean,val reason: String)

data class ReviewResponse(val reviewId: Long,
                          val movieId: Long,
                          val authorName: String,
                          val rating: Int,
                          val body: String,
                          val publishedAt: OffsetDateTime,
                          val updatedAt: OffsetDateTime,
                          val visibilityStatus: String)

data class RatingResponse(
    val averageRating: String?,
    val reviewCount: Long)

fun ReviewRow.toResponse(
    )=
        ReviewResponse(
            reviewId,movieId,authorName,rating,body,
            publishedAt.asApiTime(),
            updatedAt.asApiTime(),
            visibilityStatus
        )