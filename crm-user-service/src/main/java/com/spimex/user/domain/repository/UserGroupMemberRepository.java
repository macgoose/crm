package com.spimex.user.domain.repository;

import com.spimex.user.domain.model.user_group.UserGroupMember;
import com.spimex.user.domain.model.user_group.UserGroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserGroupMemberRepository extends JpaRepository<UserGroupMember, UserGroupMemberId> {

    List<UserGroupMember> findAllByIdGroupId(UUID groupId);

    List<UserGroupMember> findAllByIdUserId(UUID userId);
}
