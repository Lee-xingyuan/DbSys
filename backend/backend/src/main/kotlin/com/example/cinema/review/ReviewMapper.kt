package com.example.cinema.review

import com.example.cinema.common.PageRequest
import java.time.LocalDateTime
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface ReviewMapper{
    fun selectQualifyingOrderId(
        @Param("userId") userId: Long,
        @Param("movieId") movieId: Long,
        @Param("currentTime") currentTime: LocalDateTime
    ): Long?

    fun selectReviewById(reviewId: Long): ReviewRow?

    fun selectReviewByIdForUpdate(reviewId: Long): ReviewRow?

    fun selectReviewViewById(reviewId: Long): ReviewRow?

    fun selectReviewViewByUserIdAndMovieId(@Param("userId") userId: Long, @Param("movieId") movieId: Long): ReviewRow?

    fun insertReview(reviewRow: ReviewRow): Int

    fun updateOwnReview(reviewRow: ReviewRow): Int

    fun updateReviewVisibility(
        @Param("reviewId") reviewId: Long,
        @Param("visibilityStatus") visibilityStatus: String,
        @Param("updatedAt") updatedAt: LocalDateTime
    ): Int

    fun selectReviewPage(
        @Param("movieId") movieId: Long?,
        @Param("visibilityStatus") visibilityStatus: String?,
        @Param("pageRequest") pageRequest: PageRequest
    ): List<ReviewRow>

    fun countReviews(
        @Param("movieId") movieId: Long?,
        @Param("visibilityStatus") visibilityStatus: String?
    ): Long

    fun selectPublicRatingSummaryByMovieId(movieId: Long): RatingRow


}
