package com.gathr.repository;

import com.gathr.entity.DetectionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectionRuleRepository extends JpaRepository<DetectionRule, Long> {

    List<DetectionRule> findByEnabledTrue();
}


