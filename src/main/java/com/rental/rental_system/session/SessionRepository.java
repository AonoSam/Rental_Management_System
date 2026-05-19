package com.rental.rental_system.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<UserSession, Long> {

    List<UserSession> findByActiveOrderByLoggedInAtDesc(boolean active);

    List<UserSession> findByUserIdOrderByLoggedInAtDesc(Long userId);

    Optional<UserSession> findByTokenHash(String tokenHash);

    @Query("SELECT s FROM UserSession s WHERE s.active = true ORDER BY s.loggedInAt DESC")
    List<UserSession> findAllActiveSessions();

    @Modifying
    @Query("UPDATE UserSession s SET s.active = false, s.forceLoggedOut = true WHERE s.user.id = :userId AND s.active = true")
    void forceLogoutUser(Long userId);
}