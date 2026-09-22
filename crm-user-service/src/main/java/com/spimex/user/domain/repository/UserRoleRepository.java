package com.spimex.user.domain.repository;

import com.spimex.user.domain.model.user.UserRole;
import com.spimex.user.domain.model.user.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findAllByIdUserId(UUID userId);

    List<UserRole> findAllByIdRoleId(UUID roleId);
}
