package com.example.cinema.audit

import java.time.LocalDateTime

class AuditRow {
    var auditLogId = 0L; var actorUserId : Long? = null;
    var actionType =" "; var targetType =" "
    var targetId =0L; var occurredAt = LocalDateTime.MIN
    var requestId = " "; var changeSummary = " ";
    var actionReason : String?=null
}
