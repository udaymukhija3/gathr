package com.gathr.repository;

import com.gathr.entity.AuditLog;
import com.gathr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for a specific actor
     */
    List<AuditLog> findByActorOrderByTimestampDesc(User actor);

    /**
     * Find audit logs by action type
     */
    List<AuditLog> findByActionOrderByTimestampDesc(AuditLog.ActionType action);

    /**
     * Find audit logs for a specific entity
     */
    List<AuditLog> findByEntityAndEntityIdOrderByTimestampDesc(
        AuditLog.EntityType entity, Long entityId);

    /**
     * Find audit logs within a date range
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Find system actions (no actor)
     */
    @Query("SELECT a FROM AuditLog a WHERE a.actor IS NULL ORDER BY a.timestamp DESC")
    List<AuditLog> findSystemActions();

    /**
     * Find moderation actions
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.action IN ('USER_BANNED', 'USER_UNBANNED', 'REPORT_REVIEWED', 'REPORT_RESOLVED') " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findModerationActions();

    /**
     * Find audit logs by actor ID
     */
    @Query("SELECT a FROM AuditLog a WHERE a.actor.id = :actorId ORDER BY a.timestamp DESC")
    List<AuditLog> findByActorId(@Param("actorId") Long actorId);

    /**
     * Count actions by actor
     */
    long countByActor(User actor);

    /**
     * Count actions by type
     */
    long countByAction(AuditLog.ActionType action);

    /**
     * Find recent logs (last N days)
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.timestamp >= :since " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentLogs(@Param("since") LocalDateTime since);

    /**
     * Delete old audit logs (for cleanup, retention policy)
     */
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :threshold")
    void deleteOldLogs(@Param("threshold") LocalDateTime threshold);
}
