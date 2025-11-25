package com.gathr.repository;

import com.gathr.entity.SocialConnection;
import com.gathr.entity.SocialConnection.ConnectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialConnectionRepository extends JpaRepository<SocialConnection, Long> {

    Optional<SocialConnection> findBySourceUserIdAndTargetUserIdAndType(Long sourceUserId,
                                                                       Long targetUserId,
                                                                       ConnectionType type);

    List<SocialConnection> findBySourceUserId(Long sourceUserId);

    List<SocialConnection> findBySourceUserIdAndType(Long sourceUserId, ConnectionType type);

    @Query("SELECT sc FROM SocialConnection sc WHERE sc.sourceUser.id = :userId ORDER BY sc.strength DESC")
    List<SocialConnection> findAllForUserOrdered(Long userId);
}

