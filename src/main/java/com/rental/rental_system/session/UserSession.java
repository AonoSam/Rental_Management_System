package com.rental.rental_system.session;

import com.rental.rental_system.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "user_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "logged_in_at")
    private LocalDateTime loggedInAt;

    @Column(name = "logged_out_at")
    private LocalDateTime loggedOutAt;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "force_logged_out")
    private boolean forceLoggedOut;

    @PrePersist
    protected void onCreate() { loggedInAt = LocalDateTime.now(); }
}