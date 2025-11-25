package com.gathr.repository;

import com.gathr.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    @Query("SELECT d FROM UserDevice d WHERE d.user.id = :userId AND d.isActive = true")
    List<UserDevice> findActiveByUserId(Long userId);

    Optional<UserDevice> findByDeviceToken(String deviceToken);

    @Query("SELECT d FROM UserDevice d WHERE d.user.id = :userId")
    List<UserDevice> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserDevice d SET d.isActive = false WHERE d.user.id = :userId")
    void deactivateAllForUser(Long userId);

    @Modifying
    @Query("UPDATE UserDevice d SET d.isActive = false WHERE d.deviceToken = :token")
    void deactivateByToken(String token);

    @Query("SELECT DISTINCT d.deviceToken FROM UserDevice d WHERE d.user.id IN :userIds AND d.isActive = true")
    List<String> findActiveTokensByUserIds(List<Long> userIds);
}
