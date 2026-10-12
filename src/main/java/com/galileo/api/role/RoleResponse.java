package com.galileo.api.role;

import java.util.Set;

public record RoleResponse(
        Long id,
        RoleName name,
        String description,
        Set<Permission> permissions
) {
    public static RoleResponse from(RoleEntity role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                Set.copyOf(role.getPermissions())
        );
    }
}
