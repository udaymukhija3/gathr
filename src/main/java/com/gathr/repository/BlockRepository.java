package com.gathr.repository;

import com.gathr.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
       boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

       void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

       List<Block> findByBlockerId(Long blockerId);

       List<Block> findByBlockedId(Long blockedId);
}
