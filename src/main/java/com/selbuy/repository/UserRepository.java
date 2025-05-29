package com.selbuy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.selbuy.model.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email); // Add this line
    boolean existsByIdAndRoles_Name(Long userId, String roleName);
    Optional<User> findByVerificationToken(String verificationToken);
    Optional<User> findByResetToken(String resetToken); // Add this line for password reset
    List<User> findByUsernameContainingIgnoreCase(String username);

    @Modifying
    @Query("UPDATE User u SET u.balance = :balance WHERE u.id = :userId")
    void updateBalance(@Param("userId") Long userId, @Param("balance") Double balance);
}
