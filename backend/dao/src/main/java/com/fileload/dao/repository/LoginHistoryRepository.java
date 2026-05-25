package com.fileload.dao.repository;

import com.fileload.model.entity.LoginHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByUserIdOrderByTimeDesc(Long userId);
    List<LoginHistory> findByUserIdOrderByTimeDesc(Long userId, Pageable pageable);
}
