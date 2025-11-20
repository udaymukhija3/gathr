package com.gathr.repository;

import com.gathr.entity.Report;
import com.gathr.entity.User;
import com.gathr.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Find all reports against a specific user
     */
    List<Report> findByTargetUser(User targetUser);

    /**
     * Find all reports by status
     */
    List<Report> findByStatus(Report.ReportStatus status);

    /**
     * Find pending reports (for moderation queue)
     */
    List<Report> findByStatusOrderByCreatedAtDesc(Report.ReportStatus status);

    /**
     * Find all reports submitted by a user
     */
    List<Report> findByReporter(User reporter);

    /**
     * Find reports for a specific activity
     */
    List<Report> findByActivity(Activity activity);

    /**
     * Count total reports against a user
     */
    long countByTargetUser(User targetUser);

    /**
     * Count reports against a user by status
     */
    long countByTargetUserAndStatus(User targetUser, Report.ReportStatus status);

    /**
     * Count recent reports against a user (within time window)
     * Used for auto-ban logic
     */
    @Query("SELECT COUNT(r) FROM Report r " +
           "WHERE r.targetUser.id = :userId " +
           "AND r.createdAt >= :since")
    long countRecentReportsByUserId(@Param("userId") Long userId,
                                     @Param("since") LocalDateTime since);

    /**
     * Count distinct reporters against a user (prevents spam from one user)
     * Used for auto-ban logic - only count if different people reported
     */
    @Query("SELECT COUNT(DISTINCT r.reporter.id) FROM Report r " +
           "WHERE r.targetUser.id = :userId " +
           "AND r.createdAt >= :since")
    long countDistinctReportersByUserId(@Param("userId") Long userId,
                                         @Param("since") LocalDateTime since);

    /**
     * Check if user has already reported another user
     */
    boolean existsByReporterAndTargetUser(User reporter, User targetUser);

    /**
     * Find unreviewed reports (PENDING or UNDER_REVIEW) ordered by creation date
     */
    @Query("SELECT r FROM Report r " +
           "WHERE r.status IN ('PENDING', 'UNDER_REVIEW') " +
           "ORDER BY r.createdAt ASC")
    List<Report> findUnreviewedReports();

    /**
     * Find reports reviewed by a specific admin
     */
    List<Report> findByReviewedBy(User admin);

    /**
     * Count total pending reports (for admin dashboard)
     */
    long countByStatus(Report.ReportStatus status);

    /**
     * Find reports by reason
     */
    List<Report> findByReason(Report.ReportReason reason);

    /**
     * Find reports created within a date range
     */
    @Query("SELECT r FROM Report r " +
           "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY r.createdAt DESC")
    List<Report> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Get report statistics for a user (for risk scoring)
     */
    @Query("SELECT new map(" +
           "COUNT(r) as totalReports, " +
           "COUNT(DISTINCT r.reporter) as distinctReporters, " +
           "MAX(r.createdAt) as lastReportDate) " +
           "FROM Report r WHERE r.targetUser.id = :userId")
    List<Object> getReportStatsByUserId(@Param("userId") Long userId);
}
