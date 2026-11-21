package com.example.cinema.audit

import com.example.cinema.common.*
import java.time.*
import org.apache.ibatis.annotations.*

@Mapper
interface AuditMapper {
    @Insert("""
        insert into audit_log (actor_user_id,action_type,target_type,target_id,occurred_at,request_id,change_summary,action_reason)
        values (#{actorUserId,jdbcType=BIGINT},#{actionType},#{targetType},#{targetId},#{occurredAt},#{requestId},#{changeSummary},#{actionReason,jdbcType=VARCHAR})
    """)
    fun insert(@Param("actorUserId") actorUserId:Long? ,
               @Param("actionType") actionType:String?,
               @Param("targetType") targetType:String?,
               @Param("targetId") targetId:Long,
               @Param("occurredAt") occurredAt:LocalDateTime,
               @Param("requestId") requestId:String,
               @Param("changeSummary") changeSummary:String,
               @Param("actionReason") actionReason:String?
    ) :Int

}
