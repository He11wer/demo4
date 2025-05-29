package com.selbuy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.selbuy.model.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
