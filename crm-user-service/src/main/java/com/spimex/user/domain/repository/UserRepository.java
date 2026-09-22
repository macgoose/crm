package com.spimex.user.domain.repository;

import com.spimex.user.domain.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    @Query(value = """
            select case when count(*) > 0 then true else false end
              from user_role ur
              join role_permission rp on rp.role_id = ur.role_id
              join permission p on p.id = rp.permission_id
             where ur.user_id = :userId
               and p.code = :permission
            """, nativeQuery = true)
    boolean hasPermission(@Param("userId") UUID userId, @Param("permission") String permission);

}
