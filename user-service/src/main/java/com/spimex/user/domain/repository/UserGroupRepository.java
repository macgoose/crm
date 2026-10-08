package com.spimex.user.domain.repository;

import com.spimex.user.domain.model.user_group.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {

    Optional<UserGroup> findByCode(String code);

    List<UserGroup> findAllByParentIsNullOrderByName();

    List<UserGroup> findAllByParentIdOrderByName(UUID parentId);
}
