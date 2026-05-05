package com.fileload.dao.repository;

import com.fileload.model.entity.AdminAccessInvite;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAccessInviteRepository extends JpaRepository<AdminAccessInvite, Long> {
    Optional<AdminAccessInvite> findByToken(String token);
    void deleteByEmail(String email);
}

