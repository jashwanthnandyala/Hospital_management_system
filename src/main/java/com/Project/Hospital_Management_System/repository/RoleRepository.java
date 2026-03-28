package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name); // returns null if not found
}
