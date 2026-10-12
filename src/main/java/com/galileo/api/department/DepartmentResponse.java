package com.galileo.api.department;

import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String code,
        String name,
        String description,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static DepartmentResponse from(DepartmentEntity department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getDescription(),
                department.getVersion(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}
