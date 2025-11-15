package com.gathr.repository;

import com.gathr.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    @Query("SELECT a FROM Activity a " +
           "LEFT JOIN FETCH a.hub " +
           "LEFT JOIN FETCH a.createdBy " +
           "WHERE a.hub.id = :hubId " +
           "AND DATE(a.startTime) = :date ORDER BY a.startTime ASC")
    List<Activity> findByHubIdAndDate(@Param("hubId") Long hubId, @Param("date") LocalDate date);
}

