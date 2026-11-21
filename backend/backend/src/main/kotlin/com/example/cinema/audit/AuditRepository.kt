package com.example.cinema.audit

import com.example.cinema.common.checkSingleRowAffected
import java.time.LocalDateTime
import org.springframework.stereotype.Repository

@Repository
class AuditRepository(private val mapper:AuditMapper) {

    fun insert(
        actorUserId: Long?,
        actionType: String,
        targetType:String,
        targetId:Long,
        occurredAt: LocalDateTime,
        requestId:String,
        changeSummary:String,
        actionReason: String?
    ) {
        checkSingleRowAffected(mapper.insert(
            actorUserId,
            actionType,
            targetType,
            targetId,
            occurredAt,
            requestId,
            changeSummary,
            actionReason
        ))
    }


}
