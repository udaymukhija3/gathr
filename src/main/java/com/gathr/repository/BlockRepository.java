package com.gathr.repository;

import com.gathr.entity.Block;
import com.gathr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

    /**
     * Find a block relationship between two users
     */
    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    /**
     * Check if user A has blocked user B
     */
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    /**
     * Check if user A has blocked user B by ID
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
           "FROM Block b WHERE b.blocker.id = :blockerId AND b.blocked.id = :blockedId")
    boolean existsByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId,
                                          @Param("blockedId") Long blockedId);

    /**
     * Check if there's a block relationship in either direction
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
           "FROM Block b WHERE " +
           "(b.blocker.id = :userId1 AND b.blocked.id = :userId2) OR " +
           "(b.blocker.id = :userId2 AND b.blocked.id = :userId1)")
    boolean existsBetweenUsers(@Param("userId1") Long userId1,
                                @Param("userId2") Long userId2);

    /**
     * Find all users blocked by a specific user
     */
    @Query("SELECT b.blocked FROM Block b WHERE b.blocker.id = :blockerId")
    List<User> findBlockedUsersByBlockerId(@Param("blockerId") Long blockerId);

    /**
     * Find all users who have blocked a specific user
     */
    @Query("SELECT b.blocker FROM Block b WHERE b.blocked.id = :blockedId")
    List<User> findBlockersByBlockedId(@Param("blockedId") Long blockedId);

    /**
     * Get all blocked user IDs for a specific user (for filtering)
     */
    @Query("SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :userId")
    List<Long> findBlockedUserIdsByUserId(@Param("userId") Long userId);

    /**
     * Get all blocker user IDs for a specific user (users who blocked this user)
     */
    @Query("SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :userId")
    List<Long> findBlockerUserIdsByUserId(@Param("userId") Long userId);

    /**
     * Get all user IDs that should be hidden from a user (blocked + blockers)
     */
    @Query("SELECT DISTINCT CASE " +
           "WHEN b.blocker.id = :userId THEN b.blocked.id " +
           "ELSE b.blocker.id END " +
           "FROM Block b WHERE b.blocker.id = :userId OR b.blocked.id = :userId")
    List<Long> findAllBlockedAndBlockerIds(@Param("userId") Long userId);

    /**
     * Count blocks by a user
     */
    long countByBlocker(User blocker);

    /**
     * Count blocks against a user
     */
    long countByBlocked(User blocked);

    /**
     * Delete block relationship
     */
    void deleteByBlockerAndBlocked(User blocker, User blocked);
}
