package com.trackspace.classroom;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for GroupMember entity
 */
public class GroupMemberId implements Serializable {

    private Long group;
    private Long member;

    public GroupMemberId() {}

    public GroupMemberId(Long group, Long member) {
        this.group = group;
        this.member = member;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupMemberId that)) return false;
        return Objects.equals(group, that.group) && Objects.equals(member, that.member);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, member);
    }
}
