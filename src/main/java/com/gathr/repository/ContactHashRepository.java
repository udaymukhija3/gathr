package com.gathr.repository;

import com.gathr.entity.ContactHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactHashRepository extends JpaRepository<ContactHash, Long> {
    List<ContactHash> findByUserId(Long userId);
}
