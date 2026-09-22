package com.spimex.user.domain.repository;

import com.spimex.user.domain.model.role.RolePermission;
import com.spimex.user.domain.model.role.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findAllByIdRoleId(UUID roleId);

    List<RolePermission> findAllByIdPermissionId(UUID permissionId);
}
