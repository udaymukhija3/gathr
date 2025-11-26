package com.gathr.repository.spec;

import com.gathr.entity.Activity;
import com.gathr.entity.Participation;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ActivitySpecification {

    public static Specification<Activity> withFilters(Long hubId, LocalDate date, Long excludeUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by Hub
            if (hubId != null) {
                predicates.add(cb.equal(root.get("hubId"), hubId));
            }

            // Filter by Date (assuming startTime is LocalDateTime)
            if (date != null) {
                predicates.add(cb.between(root.get("startTime"),
                        date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay()));
            }

            // Exclude joined activities
            if (excludeUserId != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                var participationRoot = subquery.from(Participation.class);
                subquery.select(participationRoot.get("activity").get("id"))
                        .where(cb.equal(participationRoot.get("user").get("id"), excludeUserId));

                predicates.add(cb.not(root.get("id").in(subquery)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
