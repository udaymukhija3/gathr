package com.gathr.repository;

import com.gathr.entity.UserPhoneHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserPhoneHashRepository extends JpaRepository<UserPhoneHash, Long> {

    List<UserPhoneHash> findByUserId(Long userId);

    @Query("SELECT u.phoneHash FROM UserPhoneHash u WHERE u.user.id = :userId")
    Set<String> findPhoneHashesByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT u.user.id) FROM UserPhoneHash u WHERE u.phoneHash IN :hashes")
    Long countMutualUsers(@Param("hashes") Set<String> hashes);

    void deleteByUserId(Long userId);
}

