package com.fileload.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_audit_event")
@Getter
@Setter
@NoArgsConstructor
public class AdminAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(nullable = false, length = 255)
    private String actorEmail;

    @Column(nullable = false, length = 120)
    private String targetType;

    @Column(length = 120)
    private String targetId;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

}

