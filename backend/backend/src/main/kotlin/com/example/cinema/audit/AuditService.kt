package com.example.cinema.audit

import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


@Service
class AuditService(private val repository: AuditRepository) {

    @Transactional(propagation = Propagation.REQUIRED)
    fun recordAuditLog(
        actorUserId: Long?,
        actionType: String,
        targetType: String,
        targetId:Long,
        occurredAt: LocalDateTime,
        requestId:String,
        changeSummary:String,
        actionReason:String? = null
    ){
        require(actionType.isNotBlank() && actionType.length <= 48)
        require(targetType.isNotBlank() && targetType.length <= 40 && targetId > 0)
        require(requestId.isNotBlank() && requestId.length <= 64)
        require(changeSummary.isNotBlank() && changeSummary.length <= 500)

        val normalizedReason = actionReason?.trim()?.takeIf { it.isNotEmpty() }
        require(normalizedReason == null || normalizedReason.length <= 255)
        repository.insert(
            actorUserId ,
            actionType,
            targetType,
            targetId,
            occurredAt,
            requestId,
            changeSummary,
            normalizedReason
        )

    }

}