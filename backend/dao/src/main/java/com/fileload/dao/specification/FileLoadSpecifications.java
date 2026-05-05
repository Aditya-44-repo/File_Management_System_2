package com.fileload.dao.specification;

import com.fileload.model.dto.SearchCriteriaDTO;
import com.fileload.model.entity.FileLoad;
import com.fileload.model.entity.FileStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class FileLoadSpecifications {

    private FileLoadSpecifications() {
    }

    public static Specification<FileLoad> withCriteria(SearchCriteriaDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("archived")));

            if (criteria.fileId() != null) {
                predicates.add(cb.equal(root.get("id"), criteria.fileId()));
            }
            if (criteria.filename() != null && !criteria.filename().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("filename")), "%" + criteria.filename().toLowerCase() + "%"));
            }
            if (criteria.uploadedById() != null) {
                predicates.add(cb.equal(root.get("uploadedById"), criteria.uploadedById()));
            }
            if (criteria.status() != null && !criteria.status().isBlank()) {
                predicates.add(cb.equal(root.get("status"), FileStatus.valueOf(criteria.status().toUpperCase())));
            }
            if (criteria.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("loadDate"), criteria.startDate()));
            }
            if (criteria.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("loadDate"), criteria.endDate()));
            }
            if (criteria.recordCountMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("recordCount"), criteria.recordCountMin()));
            }
            if (criteria.recordCountMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("recordCount"), criteria.recordCountMax()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

