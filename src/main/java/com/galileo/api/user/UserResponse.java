package com.galileo.api.user;

import com.galileo.api.role.RoleName;
import com.galileo.api.team.TeamEntity;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String jobTitle,
        boolean enabled,
        TeamSummary team,
        Set<RoleName> roles,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getJobTitle(),
                user.isEnabled(),
                teamSummary(user.getTeam()),
                user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toUnmodifiableSet()),
                user.getVersion(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private static TeamSummary teamSummary(TeamEntity team) {
        if (team == null) {
            return null;
        }
        return new TeamSummary(
                team.getId(),
                team.getCode(),
                team.getName(),
                team.getDepartment().getId(),
                team.getDepartment().getName()
        );
    }

    public record TeamSummary(
            Long id,
            String code,
            String name,
            Long departmentId,
            String departmentName
    ) {
    }
}
