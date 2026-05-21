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
@Table(name = "login_history")
@Getter
@Setter
@NoArgsConstructor
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String device;

    @Column(nullable = false, length = 120)
    private String browser;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime time;

    @Column(nullable = false, length = 20)
    private String status;

    public LoginHistory(Long userId, String device, String browser, String ip, LocalDateTime time, String status) {
        this.userId = userId;
        this.device = device;
        this.browser = browser;
        this.ip = ip;
        this.time = time;
        this.status = status;
    }
}
