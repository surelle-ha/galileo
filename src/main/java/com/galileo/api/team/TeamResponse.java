package com.galileo.api.team;

import java.time.Instant;

public record TeamResponse(
        Long id,
        String code,
        String name,
        String description,
        DepartmentSummary department,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static TeamResponse from(TeamEntity team) {
        return new TeamResponse(
                team.getId(),
                team.getCode(),
                team.getName(),
                team.getDescription(),
                new DepartmentSummary(
                        team.getDepartment().getId(),
                        team.getDepartment().getCode(),
                        team.getDepartment().getName()),
                team.getVersion(),
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }

    public record DepartmentSummary(Long id, String code, String name) {
    }
}
