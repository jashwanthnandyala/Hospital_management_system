package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findAllByUserId(Long userId);
    Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);
}