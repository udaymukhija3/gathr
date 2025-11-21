package com.gathr.repository;

import com.gathr.entity.ActivityTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityTemplateRepository extends JpaRepository<ActivityTemplate, Long> {

    @Query("SELECT t FROM ActivityTemplate t WHERE t.isSystemTemplate = true")
    List<ActivityTemplate> findSystemTemplates();

    @Query("SELECT t FROM ActivityTemplate t WHERE t.createdByUser.id = :userId")
    List<ActivityTemplate> findByUserId(Long userId);

    @Query("SELECT t FROM ActivityTemplate t WHERE t.isSystemTemplate = true OR t.createdByUser.id = :userId")
    List<ActivityTemplate> findAvailableTemplatesForUser(Long userId);
}
