package com.gathr.repository;

import com.gathr.entity.InviteToken;
import com.gathr.entity.Activity;
import com.gathr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InviteTokenRepository extends JpaRepository<InviteToken, Long> {

    /**
     * Find invite token by token string
     */
    Optional<InviteToken> findByToken(String token);

    /**
     * Find all invites for a specific activity
     */
    List<InviteToken> findByActivity(Activity activity);

    /**
     * Find all invites created by a user
     */
    List<InviteToken> findByCreatedBy(User creator);

    /**
     * Find valid (non-revoked, non-expired) tokens for an activity
     */
    @Query("SELECT it FROM InviteToken it " +
           "WHERE it.activity.id = :activityId " +
           "AND it.revoked = false " +
           "AND it.expiresAt > :now")
    List<InviteToken> findValidTokensByActivityId(@Param("activityId") Long activityId,
                                                   @Param("now") LocalDateTime now);

    /**
     * Find valid token by token string
     */
    @Query("SELECT it FROM InviteToken it " +
           "WHERE it.token = :token " +
           "AND it.revoked = false " +
           "AND it.expiresAt > :now")
    Optional<InviteToken> findValidToken(@Param("token") String token,
                                          @Param("now") LocalDateTime now);

    /**
     * Check if a valid token exists
     */
    @Query("SELECT CASE WHEN COUNT(it) > 0 THEN true ELSE false END " +
           "FROM InviteToken it " +
           "WHERE it.token = :token " +
           "AND it.revoked = false " +
           "AND it.expiresAt > :now")
    boolean existsValidToken(@Param("token") String token,
                              @Param("now") LocalDateTime now);

    /**
     * Find tokens that have expired (for cleanup job)
     */
    @Query("SELECT it FROM InviteToken it " +
           "WHERE it.expiresAt < :now " +
           "AND it.revoked = false")
    List<InviteToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Find tokens by invited user
     */
    List<InviteToken> findByInvitedUser(User user);

    /**
     * Find personal invite for user and activity
     */
    Optional<InviteToken> findByActivityAndInvitedUser(Activity activity, User user);

    /**
     * Count valid invites for an activity
     */
    @Query("SELECT COUNT(it) FROM InviteToken it " +
           "WHERE it.activity.id = :activityId " +
           "AND it.revoked = false " +
           "AND it.expiresAt > :now")
    long countValidInvitesByActivityId(@Param("activityId") Long activityId,
                                        @Param("now") LocalDateTime now);

    /**
     * Count invites created by a user
     */
    long countByCreatedBy(User creator);

    /**
     * Find tokens that have reached max uses (for cleanup)
     */
    @Query("SELECT it FROM InviteToken it " +
           "WHERE it.maxUses IS NOT NULL " +
           "AND it.useCount >= it.maxUses " +
           "AND it.revoked = false")
    List<InviteToken> findMaxedOutTokens();

    /**
     * Delete expired and revoked tokens (cleanup)
     */
    @Query("DELETE FROM InviteToken it " +
           "WHERE it.expiresAt < :threshold OR it.revoked = true")
    void deleteOldTokens(@Param("threshold") LocalDateTime threshold);
}
