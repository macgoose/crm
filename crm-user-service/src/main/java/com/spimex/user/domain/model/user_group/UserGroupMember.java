package com.spimex.user.domain.model.user_group;

import com.spimex.user.domain.model.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_group_member")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGroupMember {

    @EmbeddedId
    private UserGroupMemberId id;

    @MapsId("groupId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroup group;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "added_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime addedAt;
}
